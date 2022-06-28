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
package org.lemurproject.lucindri.indexer.factory;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.lemurproject.lucindri.indexer.documentparser.CARDocumentParser;
import org.lemurproject.lucindri.indexer.documentparser.ClueWeb09DocumentParser;
import org.lemurproject.lucindri.indexer.documentparser.ClueWeb12DocumentParser;
import org.lemurproject.lucindri.indexer.documentparser.ClueWeb22DocumentParser;
import org.lemurproject.lucindri.indexer.documentparser.DocumentParser;
import org.lemurproject.lucindri.indexer.documentparser.Gov2DocumentParser;
import org.lemurproject.lucindri.indexer.documentparser.IndriGov2DocumentParser;
import org.lemurproject.lucindri.indexer.documentparser.JsonDocumentParser;
import org.lemurproject.lucindri.indexer.documentparser.MARCODocumentParser;
import org.lemurproject.lucindri.indexer.documentparser.MARCOFullDocumentParser;
import org.lemurproject.lucindri.indexer.documentparser.TextDocumentParser;
import org.lemurproject.lucindri.indexer.documentparser.TrecTextDocumentParser;
import org.lemurproject.lucindri.indexer.documentparser.WARCDocumentParser;
import org.lemurproject.lucindri.indexer.documentparser.WSJDocumentParser;
import org.lemurproject.lucindri.indexer.documentparser.WashingtonPostDocumentParser;
import org.lemurproject.lucindri.indexer.domain.IndexingConfiguration;

/**
 * Instantiates the correct document parser based on the user input for
 * property: documentFormat. To add an additional document parser, add the
 * implementation class for that parser to the docParserMap in the constructor.
 * 
 * @author cmw2
 *
 *         Nov 30, 2016
 */
public class DocumentParserFactory {

	private Map<String, Class<? extends DocumentParser>> docParserMap;

	public DocumentParserFactory() {
		docParserMap = new HashMap<>();
		docParserMap.put("text", TextDocumentParser.class);
		docParserMap.put("wsj", WSJDocumentParser.class);
		docParserMap.put("gov2", Gov2DocumentParser.class);
		docParserMap.put("indrigov2", IndriGov2DocumentParser.class);
		docParserMap.put("json", JsonDocumentParser.class);
		docParserMap.put("wapo", WashingtonPostDocumentParser.class);
		docParserMap.put("warc", WARCDocumentParser.class);
		docParserMap.put("cw09", ClueWeb09DocumentParser.class);
		docParserMap.put("cw12", ClueWeb12DocumentParser.class);
		docParserMap.put("cw22", ClueWeb22DocumentParser.class);
		docParserMap.put("car", CARDocumentParser.class);
		docParserMap.put("marco", MARCODocumentParser.class);
		docParserMap.put("marcofull", MARCOFullDocumentParser.class);
		docParserMap.put("trectext", TrecTextDocumentParser.class);
	}

	public Set<String> getDocumentFormatTypes() {
		return docParserMap.keySet();
	}

	public DocumentParser getDocumentParser(IndexingConfiguration options)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		Class<? extends DocumentParser> clazz = docParserMap.get(options.getDocumentFormat());
		DocumentParser docParser = null;
		if (clazz != null) {
			docParser = clazz.getDeclaredConstructor(IndexingConfiguration.class).newInstance(options);
		} else {
			System.out.println("ERROR: No such document parser: " + options.getDocumentFormat());
			System.out.println("Please define one of these parser types: " + getDocumentFormatTypes());
			throw new IllegalArgumentException();
		}
		return docParser;
	}

}
