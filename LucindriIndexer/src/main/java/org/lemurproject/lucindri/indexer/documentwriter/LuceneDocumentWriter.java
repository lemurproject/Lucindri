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
package org.lemurproject.lucindri.indexer.documentwriter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.similarities.IndriDirichletSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.lemurproject.lucindri.indexer.domain.IndexingConfiguration;
import org.lemurproject.lucindri.indexer.domain.ParsedDocument;
import org.lemurproject.lucindri.indexer.domain.ParsedDocumentField;
import org.lemurproject.lucindri.indexer.factory.ConfigurableAnalyzerFactory;

public class LuceneDocumentWriter implements DocumentWriter {

	private static final Logger logger = Logger.getLogger(LuceneDocumentWriter.class.getName());

	private Analyzer analyzer;
	private IndexWriter iWriter;
	private FieldType fieldType;
	private Similarity similarity;

	private List<Document> luceneDocs;
	private Field luceneField;
	private Document luceneDoc;

	public LuceneDocumentWriter(IndexingConfiguration options)
			throws IOException, ClassCastException, ClassNotFoundException {
		ConfigurableAnalyzerFactory analyzerFactory = new ConfigurableAnalyzerFactory();
		analyzer = analyzerFactory.getConfigurableAnalyzer(options);
		this.similarity = new IndriDirichletSimilarity();

		String indexDirectory = Paths.get(options.getIndexDirectory(), options.getIndexName()).toString();
		iWriter = createIndexWriter(indexDirectory, analyzer);

		fieldType = getFieldType();

		// luceneDoc = new Document();
		luceneDocs = new ArrayList<>();
	}

	/**
	 * Creates the Lucene IndexWriter for writing document to the index.
	 * 
	 * @param indexDirectory
	 * @param docParser
	 * @param analyzer
	 * @return
	 * @throws IOException
	 */
	private IndexWriter createIndexWriter(String indexDirectory, Analyzer analyzer) throws IOException {

		Path path = Paths.get(indexDirectory);
		Directory directory = FSDirectory.open(path);
		// Directory directory = new SimpleFSDirectory(path, NoLockFactory.INSTANCE);

		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		config.setOpenMode(OpenMode.CREATE);
		config.setSimilarity(similarity);
		config.setUseCompoundFile(false);
		IndexWriter iwriter = new IndexWriter(directory, config);

		return iwriter;
	}

	/**
	 * Defines how fields are stored in Lucene.
	 * 
	 * @return
	 */
	private FieldType getFieldType() {
		logger.log(Level.FINE, "Enter");
		FieldType fieldType = new FieldType();
		fieldType.setTokenized(true);
		fieldType.setStored(true);
		fieldType.setIndexOptions(org.apache.lucene.index.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
		fieldType.setStoreTermVectors(false);
		fieldType.setStoreTermVectorPositions(false);
		fieldType.setStoreTermVectorOffsets(false);
		logger.log(Level.FINE, "Exit");
		return fieldType;
	}

	public void writeDocuments(ParsedDocument parsedDoc) throws IOException {
		if (parsedDoc != null) {
			luceneDoc = new Document();

			// Add document to search engine
			for (ParsedDocumentField docField : parsedDoc.getDocumentFields()) {
				if (docField.getContent() != null) {
					if (!docField.isNumeric()) {
						luceneField = new Field(docField.getFieldName(), docField.getContent(), fieldType);
						luceneDoc.add(luceneField);
					} else {
						luceneDoc.add(new NumericDocValuesField(docField.getFieldName(),
								Long.valueOf(docField.getContent()).longValue()));
					}
				}
			}
			// iWriter.addDocument(luceneDoc);
			luceneDocs.add(luceneDoc);

		}
		if (luceneDocs.size() >= 500) {
			iWriter.addDocuments(luceneDocs);
			luceneDocs = new ArrayList<>();
		}
	}

	public void closeDocumentWriter() throws IOException {
		if (luceneDocs.size() > 0) {
			iWriter.addDocuments(luceneDocs);
		}
		// writeTotalDocLens();
		iWriter.close();
	}

//	private void writeTotalDocLens() throws IOException {
//		Map<String, Long> docLens = ((IndriDirichletSimilarity) similarity).getTotalFieldLengths();
//		Document docLenDoc = new Document();
//		Field nameField = new Field(IndriConstants.COLLECTION_TOTAL_DOCUMENT_NAME,
//				IndriConstants.COLLECTION_TOTAL_DOCUMENT_NAME, fieldType);
//		docLens.forEach((fieldName, length) -> {
//			Field field = new NumericDocValuesField(fieldName + IndriConstants.FIELD_TOTAL_SUFFIX, length);
//			docLenDoc.add(field);
//		});
//		iWriter.addDocument(docLenDoc);
//	}

}
