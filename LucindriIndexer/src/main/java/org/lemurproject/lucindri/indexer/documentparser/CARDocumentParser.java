package org.lemurproject.lucindri.indexer.documentparser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.lemurproject.lucindri.indexer.domain.IndexingConfiguration;
import org.lemurproject.lucindri.indexer.domain.ParsedDocument;
import org.lemurproject.lucindri.indexer.domain.ParsedDocumentField;
import org.xml.sax.SAXException;

import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;

public class CARDocumentParser extends DocumentParser {

	private final static String BODY_FIELD = "body";

	private final FileInputStream stream;
	private final Iterator<Data.Paragraph> iter;

	private boolean atEOF;
	private int docNum;
	private List<String> fieldsToIndex;
	private boolean indexFullText;

	public CARDocumentParser(IndexingConfiguration options) throws FileNotFoundException {
		stream = new FileInputStream(new File(options.getDataDirectory()));
		iter = DeserializeData.iterableParagraphs(stream).iterator();
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
		if (iter.hasNext() || !atEOF) {
			return true;
		}
		return false;
	}

	@Override
	public ParsedDocument getNextDocument() throws IOException, SAXException {
		System.setProperty("file.encoding", "UTF-8");
		Data.Paragraph p;
		p = iter.next();

		String content = p.getTextOnly();

		ParsedDocument doc = new ParsedDocument();
		doc.setDocumentFields(new ArrayList<>());

		ParsedDocumentField internalIdField = new ParsedDocumentField(INTERNALID_FIELD, String.valueOf(docNum), false);
		doc.getDocumentFields().add(internalIdField);

		ParsedDocumentField externalIdField = new ParsedDocumentField(EXTERNALID_FIELD, p.getParaId(), false);
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

		if (!iter.hasNext()) {
			atEOF = true;
		}

		docNum++;
		return doc;
	}

}
