package org.lemurproject.lucindri.indexer.documentparser;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringJoiner;
import java.util.zip.GZIPInputStream;

import org.apache.lucene.analysis.Analyzer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;
import org.lemurproject.lucindri.indexer.domain.IndexingConfiguration;
import org.lemurproject.lucindri.indexer.domain.ParsedDocument;
import org.lemurproject.lucindri.indexer.domain.ParsedDocumentField;
import org.lemurproject.lucindri.indexer.factory.ConfigurableAnalyzerFactory;
import org.xml.sax.SAXException;

public class ClueWeb12DocumentParser extends DocumentParser {

	private final static String EXTERNALID_FIELD = "externalId";
	private final static String ID_FIELD = "internalId";
	private final static String BODY_FIELD = "body";
	private final static String TITLE_FIELD = "title";
	private final static String HEADING_FIELD = "heading";
	private final static String URL_FIELD = "url";

	private static final byte MASK_THREE_BYTE_CHAR = (byte) (0xE0);
	private static final byte MASK_TWO_BYTE_CHAR = (byte) (0xC0);
	private static final byte MASK_TOPMOST_BIT = (byte) (0x80);
	private static final byte MASK_BOTTOM_SIX_BITS = (byte) (0x1F);
	private static final byte MASK_BOTTOM_FIVE_BITS = (byte) (0x3F);
	private static final byte MASK_BOTTOM_FOUR_BITS = (byte) (0x0F);

	private final static String NEWLINE = "\n";

	private int docNum;
	private Iterator<File> fileIterator;
	private DataInputStream stream;
	private String nextLine;
	private Analyzer analyzer;
	private List<String> fieldsToIndex;
	private boolean indexFullText;

	Document htmlDoc;
	ParsedDocument doc;
	ParsedDocumentField externalIdField;
	ParsedDocumentField internalIdField;
	ParsedDocumentField bodyField;
	ParsedDocumentField titleField;
	ParsedDocumentField headingField;
	ParsedDocumentField urlField;
	ParsedDocumentField fullTextField;

	public ClueWeb12DocumentParser(IndexingConfiguration options) throws IOException {
		List<File> files = new ArrayList<>();
		listFiles(options.getDataDirectory(), files);
		fileIterator = files.iterator();
		getNextStream();
		ConfigurableAnalyzerFactory analyzerFactory = new ConfigurableAnalyzerFactory();
		analyzer = analyzerFactory.getConfigurableAnalyzer(options);
		docNum = 0;
		fieldsToIndex = options.getIndexFields();
		indexFullText = options.isIndexFullText();
	}

	private void getNextStream() throws IOException {
		if (fileIterator.hasNext()) {
			File nextFile = fileIterator.next();
			stream = new DataInputStream(new GZIPInputStream(
					Files.newInputStream(Paths.get(nextFile.getAbsolutePath()), StandardOpenOption.READ)));

			// Remove warinfo record
			String line = null;
			while ((line = readLineFromInputStream(stream)) != null) {
				if (line.startsWith("description")) {
					break;
				}
			}
		} else {
			stream = null;
		}
	}

	public static void listFiles(String directoryName, List<File> files) {
		File directory = new File(directoryName);

		// get all the files from a directory
		File[] fList = directory.listFiles();
		for (File file : fList) {
			if (file.isFile()) {
				files.add(file);
			} else if (file.isDirectory()) {
				listFiles(file.getAbsolutePath(), files);
			}
		}
	}

	@Override
	public boolean hasNextDocument() {
		return fileIterator.hasNext() || stream != null;
	}

	private String readLineFromInputStream(DataInputStream in) throws IOException {
		StringBuilder retString = new StringBuilder();

		boolean keepReading = true;
		try {
			do {
				char thisChar = 0;
				byte readByte = in.readByte();

				// check to see if it's a multibyte character
				if ((readByte & MASK_THREE_BYTE_CHAR) == MASK_THREE_BYTE_CHAR) {
					// need to read the next 2 bytes
					if (in.available() < 2) {
						// treat these all as individual characters
						retString.append((char) readByte);
						int numAvailable = in.available();
						for (int i = 0; i < numAvailable; i++) {
							retString.append((char) (in.readByte()));
						}
						continue;
					}
					byte secondByte = in.readByte();
					byte thirdByte = in.readByte();
					// ensure the topmost bit is set
					if (((secondByte & MASK_TOPMOST_BIT) != MASK_TOPMOST_BIT)
							|| ((thirdByte & MASK_TOPMOST_BIT) != MASK_TOPMOST_BIT)) {
						// treat these as individual characters
						retString.append((char) readByte);
						retString.append((char) secondByte);
						retString.append((char) thirdByte);
						continue;
					}
					int finalVal = (thirdByte & MASK_BOTTOM_FIVE_BITS) + 64 * (secondByte & MASK_BOTTOM_FIVE_BITS)
							+ 4096 * (readByte & MASK_BOTTOM_FOUR_BITS);
					thisChar = (char) finalVal;
				} else if ((readByte & MASK_TWO_BYTE_CHAR) == MASK_TWO_BYTE_CHAR) {
					// need to read next byte
					if (in.available() < 1) {
						// treat this as individual characters
						retString.append((char) readByte);
						continue;
					}
					byte secondByte = in.readByte();
					if ((secondByte & MASK_TOPMOST_BIT) != MASK_TOPMOST_BIT) {
						retString.append((char) readByte);
						retString.append((char) secondByte);
						continue;
					}
					int finalVal = (secondByte & MASK_BOTTOM_FIVE_BITS) + 64 * (readByte & MASK_BOTTOM_SIX_BITS);
					thisChar = (char) finalVal;
				} else {
					// interpret it as a single byte
					thisChar = (char) readByte;
				}

				if (thisChar == '\n') {
					keepReading = false;
				} else {
					retString.append(thisChar);
				}
			} while (keepReading);
		} catch (EOFException eofEx) {
			return null;
		}

		if (retString.length() == 0) {
			return "";
		}

		return retString.toString();
	}

	private byte[] readNextRecord(DataInputStream in, StringBuilder headerBuffer, String version) throws IOException {
		if (in == null || headerBuffer == null) {
			throw new NoSuchElementException();
		}

		String line = null;
		boolean foundMark = false;
		boolean inHeader = true;
		byte[] retContent = null;

		// cannot be using a buffered reader here!!!!
		// just read the header
		// first - find our WARC header
		while ((!foundMark) && ((line = readLineFromInputStream(in)) != null)) {
			if (line.startsWith(version)) {
				foundMark = true;
			}
		}

		// no WARC mark?
		if (!foundMark) {
			throw new NoSuchElementException();
		}

		// then read to the first newline
		// make sure we get the content length here
		int contentLength = -1;
		int foundContentLength = 0;
		while (foundContentLength < 2 && inHeader && ((line = readLineFromInputStream(in)) != null)) {
			if ((line.trim().length() == 0) && foundContentLength == 2) {
				inHeader = false;
			} else {
				if (line.startsWith("conformsTo")) {
					break;
				}
				headerBuffer.append(line);
				headerBuffer.append(NEWLINE);
				String[] thisHeaderPieceParts = line.split(":", 2);
				if (thisHeaderPieceParts.length == 2) {
					if (thisHeaderPieceParts[0].toLowerCase(Locale.US).startsWith("content-length")) {
						foundContentLength++;
						try {
							contentLength = Integer.parseInt(thisHeaderPieceParts[1].trim());
						} catch (NumberFormatException nfEx) {
							contentLength = -1;
						}
					}
				}
			}
		}

		if (contentLength < 0) {
			throw new NoSuchElementException();
		}

		// now read the bytes of the content
		retContent = new byte[contentLength];
		int totalWant = contentLength;
		int totalRead = 0;
		while (totalRead < contentLength) {
			try {
				int numRead = in.read(retContent, totalRead, totalWant);
				if (numRead < 0) {
					throw new NoSuchElementException();
				} else {
					totalRead += numRead;
					totalWant = contentLength - totalRead;
				} // end if (numRead < 0) / else
			} catch (EOFException eofEx) {
				// resize to what we have
				if (totalRead > 0) {
					byte[] newReturn = new byte[totalRead];
					System.arraycopy(retContent, 0, newReturn, 0, totalRead);
					return newReturn;
				} else {
					throw new NoSuchElementException();
				}
			} // end try/catch (EOFException)
		} // end while (totalRead < contentLength)

		return retContent;
	}

	@Override
	public ParsedDocument getNextDocument() throws IOException, SAXException {
		if (stream == null) {
			getNextStream();
		}

		if (stream != null) {
			StringBuilder recordHeader = new StringBuilder();
			byte[] recordContent = null;
			try {
				recordContent = readNextRecord(stream, recordHeader, "WARC/1.0");
			} catch (Exception e) {
				System.out.println("ClueWebParser docNum: " + docNum);
				stream = null;
				return null;
			}

			// extract out our header information
			String thisHeaderString = recordHeader.toString();
			String[] headerLines = thisHeaderString.split("\n");
			String trecID = String.valueOf(docNum);
			String url = "";
			String warcType = "";
			for (int i = 0; i < headerLines.length; i++) {
				String[] pieces = headerLines[i].split(":", 2);
				if (pieces.length > 1) {
					String thisKey = pieces[0].trim();
					String thisValue = pieces[1].trim();

					// check for known keys
					if (thisKey.equals("WARC-Type")) {
						warcType = thisValue;
					} else if (thisKey.equals("WARC-TREC-ID")) {
						trecID = thisValue;
					} else if (thisKey.equals("WARC-Target-URI")) {
						url = thisValue;
					}
				}
			}

			if (recordContent != null && warcType.equals("response")) {
				try {
					docNum++;
					String docString = new String(recordContent, StandardCharsets.UTF_8);
					htmlDoc = Jsoup.parse(docString);
					String cleanText = null;

					if (trecID == null || trecID.length() == 0) {
						trecID = String.valueOf(docNum);
					}

					doc = new ParsedDocument();
					doc.setDocumentFields(new ArrayList<ParsedDocumentField>());

					externalIdField = new ParsedDocumentField(EXTERNALID_FIELD, trecID, false);
					doc.getDocumentFields().add(externalIdField);

					internalIdField = new ParsedDocumentField(ID_FIELD, String.valueOf(docNum), false);
					doc.getDocumentFields().add(internalIdField);

					if (fieldsToIndex.contains(BODY_FIELD)) {
						if (cleanText == null) {
							cleanText = Jsoup.clean(docString, Whitelist.simpleText());
						}
						bodyField = new ParsedDocumentField(BODY_FIELD, cleanText, false);
						doc.getDocumentFields().add(bodyField);
					}

					if (fieldsToIndex.contains(TITLE_FIELD)) {
						String title = "";
						Elements titleElements = htmlDoc.getElementsByTag("title");
						if (titleElements != null && titleElements.size() > 0) {
							Element element = titleElements.get(0);
							title = element.toString();
							title = Jsoup.clean(title, Whitelist.simpleText());
						}
						titleField = new ParsedDocumentField(TITLE_FIELD, title, false);
						doc.getDocumentFields().add(titleField);
					}

					if (fieldsToIndex.contains(HEADING_FIELD)) {
						Elements headerElements = htmlDoc.getElementsByTag("h1");
						StringJoiner headersBuffer = new StringJoiner(" ");
						if (headerElements != null && headerElements.size() > 0) {
							for (Element headerElement : headerElements) {
								headersBuffer.add(headerElement.toString());
							}
						}
						headingField = new ParsedDocumentField(HEADING_FIELD, headersBuffer.toString(), false);
						doc.getDocumentFields().add(headingField);
					}

					if (fieldsToIndex.contains(URL_FIELD)) {
						urlField = new ParsedDocumentField(URL_FIELD, url, false);
						doc.getDocumentFields().add(urlField);
					}

					// Index fullText (catch-all) field
					if (indexFullText) {
						if (cleanText == null) {
							cleanText = Jsoup.clean(docString, Whitelist.simpleText());
						}
						fullTextField = new ParsedDocumentField(FULLTEXT_FIELD, cleanText, false);
						doc.getDocumentFields().add(fullTextField);
					}

					return doc;
				} catch (Exception e) {
					System.out.println("Could not parse document: " + trecID);
				}
			}
		}
		return null;
	}

}
