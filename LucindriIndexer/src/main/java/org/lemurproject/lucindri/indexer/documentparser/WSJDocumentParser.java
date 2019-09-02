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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.lucene.benchmark.byTask.feeds.DocData;
import org.apache.lucene.benchmark.byTask.feeds.NoMoreDataException;
import org.apache.lucene.benchmark.byTask.feeds.TrecContentSourceExtended;
import org.apache.lucene.benchmark.byTask.utils.Config;
import org.lemurproject.lucindri.indexer.domain.IndexingConfiguration;
import org.lemurproject.lucindri.indexer.domain.ParsedDocument;
import org.lemurproject.lucindri.indexer.domain.ParsedDocumentField;

/**
 * @author cmw2
 *
 *         Dec 19, 2016
 */
public class WSJDocumentParser extends DocumentParser {

	private final static String DATE_FIELD = "date";
	private final static String SUBJECT_FIELD = "subject";
	private final static String TITLE_FIELD = "title";
	private final static String BODY_FIELD = "body";

	private boolean hasNextDocument = true;
	private TrecContentSourceExtended dcsr;
	private List<String> fieldsToIndex;
	private boolean indexFullText;

	/**
	 * @throws IOException
	 * 
	 */
	public WSJDocumentParser(IndexingConfiguration options) throws IOException {
		dcsr = new TrecContentSourceExtended();
		Properties pr = new Properties();
		pr.setProperty("work.dir", (new File(options.getDataDirectory())).getAbsolutePath());
		pr.setProperty("docs.dir", (new File(options.getDataDirectory())).getAbsolutePath());
		pr.setProperty("trec.doc.parser", "org.apache.lucene.benchmark.byTask.feeds.TrecWSJParser");
		pr.setProperty("content.source.forever", "false");
		pr.setProperty("content.source.log.step", "100");
		pr.setProperty("content.source.verbose", "true");
		pr.setProperty("content.source.excludeIteration", "true");
		Config cr = new Config(pr);
		dcsr.setConfig(cr);
		dcsr.resetInputs();
		fieldsToIndex = options.getIndexFields();
		indexFullText = options.isIndexFullText();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.lemurproject.sifaka.buildindex.lucene.documentparser.DocumentParser#
	 * hasNextDocument()
	 */
	@Override
	public boolean hasNextDocument() {
		return hasNextDocument;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.lemurproject.sifaka.buildindex.lucene.documentparser.DocumentParser#
	 * getNextDocument()
	 */
	@Override
	public ParsedDocument getNextDocument() throws IOException {
		ParsedDocument doc = null;

		DocData d = new DocData();
		try {
			d = dcsr.getNextDocData(d);

			doc = new ParsedDocument();
			doc.setDocumentFields(new ArrayList<>());

			ParsedDocumentField externalIdField = new ParsedDocumentField(EXTERNALID_FIELD, d.getName(), false);
			doc.getDocumentFields().add(externalIdField);

			ParsedDocumentField internalIdField = new ParsedDocumentField(INTERNALID_FIELD,
					d.getProps().getProperty("internalId"), false);
			doc.getDocumentFields().add(internalIdField);

			if (fieldsToIndex.contains(DATE_FIELD)) {
				ParsedDocumentField dateField = new ParsedDocumentField(DATE_FIELD,
						d.getProps().getProperty("headline"), false);
				doc.getDocumentFields().add(dateField);
			}

			if (fieldsToIndex.contains(SUBJECT_FIELD)) {
				ParsedDocumentField subjectField = new ParsedDocumentField(SUBJECT_FIELD,
						d.getProps().getProperty("subject"), false);
				doc.getDocumentFields().add(subjectField);
			}

			if (fieldsToIndex.contains(TITLE_FIELD)) {
				ParsedDocumentField titleField = new ParsedDocumentField(TITLE_FIELD, d.getTitle(), false);
				doc.getDocumentFields().add(titleField);
			}

			if (fieldsToIndex.contains(BODY_FIELD)) {
				ParsedDocumentField bodyField = new ParsedDocumentField(BODY_FIELD,
						String.join("\n", d.getTitle(), d.getProps().getProperty("summary"), d.getBody()), false);
				doc.getDocumentFields().add(bodyField);
			}

			if (indexFullText) {
				String all = String.join(System.lineSeparator(), d.getTitle(), d.getBody());
				ParsedDocumentField allField = new ParsedDocumentField(FULLTEXT_FIELD, all, false);
				doc.getDocumentFields().add(allField);
			}

		} catch (NoMoreDataException e) {
			hasNextDocument = false;
			dcsr.close();
		}

		return doc;
	}

}
