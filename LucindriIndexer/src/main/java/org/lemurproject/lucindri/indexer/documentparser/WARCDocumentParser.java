package org.lemurproject.lucindri.indexer.documentparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

public class WARCDocumentParser extends DocumentParser {

	private final static String EXTERNALID_FIELD = "externalId";
	private final static String ID_FIELD = "internalId";
	private final static String BODY_FIELD = "body";
	private final static String TITLE_FIELD = "title";
	private final static String HEADING_FIELD = "heading";
	private final static String URL_FIELD = "url";

	private int docNum;
	private Iterator<File> fileIterator;
	private BufferedReader br;
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

	public WARCDocumentParser(IndexingConfiguration options) throws IOException {
		// File folder = Paths.get(options.getDataDirectory()).toFile();
		List<File> files = new ArrayList<>();
		listFiles(options.getDataDirectory(), files);
		fileIterator = files.iterator();
		getNextScanner();
		nextLine = "";
		ConfigurableAnalyzerFactory analyzerFactory = new ConfigurableAnalyzerFactory();
		analyzer = analyzerFactory.getConfigurableAnalyzer(options);
		docNum = 0;
		fieldsToIndex = options.getIndexFields();
		indexFullText = options.isIndexFullText();
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

	private void getNextScanner() throws IOException {
		if (fileIterator.hasNext()) {
			File nextFile = fileIterator.next();
			InputStream fileStream = new FileInputStream(nextFile);
			InputStream gzipStream = new GZIPInputStream(fileStream);
			Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
			br = new BufferedReader(decoder);
		} else {
			br = null;
		}
	}

	@Override
	public boolean hasNextDocument() {
		return fileIterator.hasNext() || nextLine != null;
	}

	@Override
	public ParsedDocument getNextDocument() throws IOException, SAXException {
		String trecID = "";
		String url = "";
		Pattern responsePattern = Pattern.compile("^(HTTP|http)/(1|2)\\.\\d \\d{3}(.|\\s)+$");
		while (br != null) {
			if ((nextLine = br.readLine()) == null) {
				br.close();
				getNextScanner();
				if (br != null) {
					nextLine = br.readLine();
				}
			}
			if (nextLine != null) {
				docNum++;
				Matcher matcher;

				StringJoiner docBuffer = null;

				// Get values from header
				if (nextLine.startsWith("WARC-TREC-ID:")) {
					trecID = nextLine.split(" ")[1];
					// isSpam = spamFilter.isSpam(trecID);
				}
				// if (!isSpam) {
				if (nextLine.startsWith("WARC-Target-URI:")) {
					url = nextLine.split(" ")[1];
				}

				if (nextLine.startsWith("h") || nextLine.startsWith("H")) {
					matcher = responsePattern.matcher(nextLine);
					if (matcher.find()) {
						docBuffer = new StringJoiner("");
						docBuffer.add(nextLine);
					}
				}

				while (docBuffer != null && ((nextLine = br.readLine()) != null) /* && !(nextLine.length() == 9) */
						&& !nextLine.startsWith("WARC/")) {
					// nextLine = nextLine.replaceAll("\\&\\#[0-9]+\\;", "");
					docBuffer.add(nextLine);
					docBuffer.add("\n");
				}

				if (docBuffer != null) {
					try {
						htmlDoc = Jsoup.parse(docBuffer.toString());
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
								cleanText = Jsoup.clean(docBuffer.toString(), Whitelist.simpleText());
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
								cleanText = Jsoup.clean(docBuffer.toString(), Whitelist.simpleText());
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
			// }
		}
		return null;
	}

}
