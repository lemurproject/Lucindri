/*
 * ===============================================================================================
 * Copyright (c) 2017 Carnegie Mellon University and University of Massachusetts. All Rights
 * Reserved.
 *
 * Use of the Lemur Toolkit for Language Modeling and Information Retrieval is subject to the terms
 * of the software license set forth in the LICENSE file included with this software, and also
 * available at http://www.lemurproject.org/license.html
 *
 * ================================================================================================
 */
package org.lemurproject.lucindri.indexer.service;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lemurproject.lucindri.indexer.documentparser.DocumentParser;
import org.lemurproject.lucindri.indexer.documentwriter.DocumentWriter;
import org.lemurproject.lucindri.indexer.documentwriter.LuceneDocumentWriter;
import org.lemurproject.lucindri.indexer.documentwriter.SolrDocumentWriter;
import org.lemurproject.lucindri.indexer.domain.IndexingConfiguration;
import org.lemurproject.lucindri.indexer.domain.ParsedDocument;
import org.lemurproject.lucindri.indexer.factory.DocumentParserFactory;
import org.xml.sax.SAXException;

public class IndexServiceImpl implements IndexService {

	private static final Logger logger = Logger.getLogger(IndexServiceImpl.class.getName());

	@Override
	public void buildIndex(IndexingConfiguration indexingConfig) throws IOException, InstantiationException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
			SecurityException, SAXException, ClassCastException, ClassNotFoundException {
		logger.log(Level.FINE, "Enter");
		long startTime = System.currentTimeMillis();

		DocumentParserFactory docParserFactory = new DocumentParserFactory();
		DocumentParser docParser = docParserFactory.getDocumentParser(indexingConfig);

		List<DocumentWriter> docWriters = new ArrayList<>();

		DocumentWriter docWriter = new SolrDocumentWriter(indexingConfig);
		if (indexingConfig.getIndexPlatform() != null && indexingConfig.getIndexPlatform().equalsIgnoreCase("solr")) {
			docWriter = new SolrDocumentWriter(indexingConfig);
		} else {
			String indexDirPath = Paths.get(indexingConfig.getIndexDirectory(), indexingConfig.getIndexName())
					.toString();
			File rootDir = new File(indexDirPath);
			rootDir.mkdir();
			docWriter = new LuceneDocumentWriter(indexingConfig);
		}
		docWriters.add(docWriter);

		// Parse documents and add annotations
		System.out.println("Indexing started...");
		int docCount = 0;
		ParsedDocument parsedDoc;
		long endTime = System.currentTimeMillis();
		long elapsedTime = (endTime - startTime) / 1000;
		while (docParser.hasNextDocument()) {
			parsedDoc = docParser.getNextDocument();
			if (parsedDoc != null) {
				docCount++;
				for (DocumentWriter writer : docWriters) {
					writer.writeDocuments(parsedDoc);
				}
				if (docCount % 1000 == 0) {
					endTime = System.currentTimeMillis();
					elapsedTime = (endTime - startTime) / 1000;
					System.out.println(LocalTime.MIN.plusSeconds(elapsedTime).toString() + ": " + docCount
							+ " documents indexed...");
				}
			}

		}

		for (DocumentWriter writer : docWriters) {
			writer.closeDocumentWriter();
		}

		System.out.println("INDEX COMPLETE: " + docCount + " documents indexed");

		endTime = System.currentTimeMillis();
		elapsedTime = (endTime - startTime) / 1000;

		System.out.println("Indexing time: " + LocalTime.MIN.plusSeconds(elapsedTime).toString());
		logger.log(Level.INFO, "Lucene Indexing time: " + LocalTime.MIN.plusSeconds(elapsedTime).toString());
		logger.log(Level.FINE, "Exit");
	}

}
