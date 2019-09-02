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
package org.lemurproject.lucindri.indexer.factory;

import org.apache.lucene.analysis.Analyzer;
import org.lemurproject.lucindri.analyzer.EnglishAnalyzerConfigurable;
import org.lemurproject.lucindri.analyzer.EnglishAnalyzerConfigurable.StemmerType;
import org.lemurproject.lucindri.indexer.domain.IndexingConfiguration;

/**
 * Instantiates a Lucene analyzer based on the user input for properties:
 * stemmer, removeStopwords, and ignoreCase.
 * 
 * @author cmw2
 *
 *         Nov 30, 2016
 */
public class ConfigurableAnalyzerFactory {

	private static Analyzer analyzer;

	public static Analyzer getConfigurableAnalyzer(IndexingConfiguration options) {
		if (analyzer == null) {
			EnglishAnalyzerConfigurable an = new EnglishAnalyzerConfigurable();
			an.setLowercase(options.isIgnoreCase());
			an.setStopwordRemoval(options.isRemoveStopwords());
			StemmerType stemmerType = EnglishAnalyzerConfigurable.StemmerType.NONE;
			if (options.getStemmer().equalsIgnoreCase("kstem") || options.getStemmer().equalsIgnoreCase("krovetz")) {
				stemmerType = EnglishAnalyzerConfigurable.StemmerType.KSTEM;
			} else if (options.getStemmer().equalsIgnoreCase("porter")) {
				stemmerType = EnglishAnalyzerConfigurable.StemmerType.PORTER;
			}
			an.setStemmer(stemmerType);
			analyzer = an;
		}
		return analyzer;
	}

}
