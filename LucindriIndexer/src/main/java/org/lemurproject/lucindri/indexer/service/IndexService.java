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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.lemurproject.lucindri.indexer.domain.IndexingConfiguration;
import org.xml.sax.SAXException;

public interface IndexService {

	/**
	 * 
	 * Creates an index using the properties defined in the file at
	 * optionsPathString. First, the analyzer, document parser, and annotators are
	 * instantiating using the IndexProperties. Then the documents are parsed and
	 * run through the annotators. Finally, the content and annotations for each
	 * document are put into the search engine.
	 * 
	 * @param optionsPathString - the path to the properties files containing the
	 *                          IndexOptions properties
	 * @return IndexDetails Object with information about the index
	 * @throws IOException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	void buildIndex(IndexingConfiguration indexingConfig) throws IOException, InstantiationException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
			SecurityException, SAXException, ClassCastException, ClassNotFoundException;

}
