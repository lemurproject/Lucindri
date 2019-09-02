/*
 * ===============================================================================================
 * Copyright (c) 2016 Carnegie Mellon University and University of Massachusetts. All Rights
 * Reserved.
 *
 * Use of the Lemur Toolkit for Language Modeling and Information Retrieval is subject to the terms
 * of the software license set forth in the LICENSE file included with this software, and also
 * available at http://www.lemurproject.org/license.html
 *
 * ================================================================================================
 */
package org.lemurproject.lucindri.indexer.documentparser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.lemurproject.lucindri.indexer.domain.IndexingConfiguration;
import org.lemurproject.lucindri.indexer.domain.ParsedDocument;
import org.lemurproject.lucindri.indexer.domain.ParsedDocumentField;

/**
 * An implementation of the DocumentParser which parser plain text files.
 * 
 * @author cmw2
 *
 *         Dec 1, 2016
 */
public class TextDocumentParser extends DocumentParser {

	private final static String BODY_FIELD = "body";

	private int docNum;
	private File[] files;
	private List<String> fieldsToIndex;
	private boolean indexFullText;

	public TextDocumentParser(IndexingConfiguration options) {
		files = new File(options.getDataDirectory()).listFiles();
		docNum = 0;
		fieldsToIndex = options.getIndexFields();
		if (fieldsToIndex == null) {
			fieldsToIndex = new ArrayList<String>();
		}
		indexFullText = options.isIndexFullText();
	}

	@Override
	public boolean hasNextDocument() {
		if (docNum < files.length) {
			return true;
		}
		return false;
	}

	@Override
	public ParsedDocument getNextDocument() throws IOException {
		String content = new String(Files.readAllBytes(Paths.get(files[docNum].getPath())));

		ParsedDocument doc = new ParsedDocument();
		doc.setDocumentFields(new ArrayList<>());

		ParsedDocumentField internalIdField = new ParsedDocumentField(INTERNALID_FIELD, String.valueOf(docNum), false);
		doc.getDocumentFields().add(internalIdField);

		ParsedDocumentField externalIdField = new ParsedDocumentField(EXTERNALID_FIELD, files[docNum].getName(), false);
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
	}

}
