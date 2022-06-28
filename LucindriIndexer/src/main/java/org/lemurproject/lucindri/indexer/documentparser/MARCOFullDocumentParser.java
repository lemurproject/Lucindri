package org.lemurproject.lucindri.indexer.documentparser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.lemurproject.lucindri.indexer.domain.IndexingConfiguration;
import org.lemurproject.lucindri.indexer.domain.ParsedDocument;
import org.lemurproject.lucindri.indexer.domain.ParsedDocumentField;
import org.xml.sax.SAXException;

public class MARCOFullDocumentParser extends DocumentParser {

	private final static String BODY_FIELD = "body";

	private BufferedReader br;

	private boolean atEOF;
	private int docNum;
	private List<String> fieldsToIndex;
	private boolean indexFullText;

	public MARCOFullDocumentParser(IndexingConfiguration options) throws FileNotFoundException {
		br = new BufferedReader(new InputStreamReader(new FileInputStream(options.getDataDirectory())));
		atEOF = false;
		docNum = 0;
		fieldsToIndex = options.getIndexFields();
		if (fieldsToIndex == null) {
			fieldsToIndex = new ArrayList<String>();
		}
		indexFullText = options.isIndexFullText();
	}

	@Override
	public boolean hasNextDocument() {
		if (br != null) {
			return true;
		}
		return false;
	}

	@Override
	public ParsedDocument getNextDocument() throws IOException, SAXException {
		System.setProperty("file.encoding", "UTF-8");
		String nextLine = br.readLine();
		if (nextLine != null) {
			String[] docParts = nextLine.split("\\s+", 3);

			String url = docParts[1];
			String fullText = docParts[2];

			ParsedDocument doc = new ParsedDocument();
			doc.setDocumentFields(new ArrayList<>());

			ParsedDocumentField internalIdField = new ParsedDocumentField(INTERNALID_FIELD, String.valueOf(docNum),
					false);
			doc.getDocumentFields().add(internalIdField);

			ParsedDocumentField externalIdField = new ParsedDocumentField(EXTERNALID_FIELD, docParts[0], false);
			doc.getDocumentFields().add(externalIdField);

			String titleAndBody[] = fullText.split("\\s\\s\\s\\s+");
			String title = "";
			String body = fullText;
			if (titleAndBody.length > 1) {
				title = titleAndBody[0];
				body = titleAndBody[1];
			}

			if (fieldsToIndex.contains(BODY_FIELD)) {
				ParsedDocumentField bodyField = new ParsedDocumentField(BODY_FIELD, body, false);
				bodyField.setLength(countTokens(body, BODY_FIELD));
				doc.getDocumentFields().add(bodyField);
			}

			if (fieldsToIndex.contains(TITLE_FIELD)) {
				ParsedDocumentField titleField = new ParsedDocumentField(TITLE_FIELD, title, false);
				doc.getDocumentFields().add(titleField);
			}

			if (fieldsToIndex.contains(URL_FIELD)) {
				ParsedDocumentField urlField = new ParsedDocumentField(URL_FIELD, url, false);
				doc.getDocumentFields().add(urlField);
			}

			if (indexFullText) {
				// String fullText = String.join(" ", title, content);
				ParsedDocumentField fullTextField = new ParsedDocumentField(FULLTEXT_FIELD, fullText, false);
				fullTextField.setLength(countTokens(fullText, FULLTEXT_FIELD));
				doc.getDocumentFields().add(fullTextField);
			}

			docNum++;
			return doc;
		} else {
			br = null;
			return null;
		}
	}

}
