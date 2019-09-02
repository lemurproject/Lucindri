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

public class MARCODocumentParser extends DocumentParser {

	private final static String BODY_FIELD = "body";

	private BufferedReader br;

	private boolean atEOF;
	private int docNum;
	private List<String> fieldsToIndex;
	private boolean indexFullText;

	public MARCODocumentParser(IndexingConfiguration options) throws FileNotFoundException {
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
			String[] docParts = nextLine.split("\t");

			String content = docParts[1];

			ParsedDocument doc = new ParsedDocument();
			doc.setDocumentFields(new ArrayList<>());

			ParsedDocumentField internalIdField = new ParsedDocumentField(INTERNALID_FIELD, String.valueOf(docNum),
					false);
			doc.getDocumentFields().add(internalIdField);

			ParsedDocumentField externalIdField = new ParsedDocumentField(EXTERNALID_FIELD, docParts[0], false);
			doc.getDocumentFields().add(externalIdField);

			if (fieldsToIndex.contains(BODY_FIELD)) {
				ParsedDocumentField bodyField = new ParsedDocumentField(BODY_FIELD, content, false);
				bodyField.setLength(countTokens(content, BODY_FIELD));
				doc.getDocumentFields().add(bodyField);
			}

			if (indexFullText) {
				ParsedDocumentField fullTextField = new ParsedDocumentField(FULLTEXT_FIELD, content, false);
				fullTextField.setLength(countTokens(content, FULLTEXT_FIELD));
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
