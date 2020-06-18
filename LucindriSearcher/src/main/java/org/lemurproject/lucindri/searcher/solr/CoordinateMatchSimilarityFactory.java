/*
 * ===============================================================================================
 * Copyright (c) 2020 Carnegie Mellon University and University of Massachusetts. All Rights
 * Reserved.
 *
 * Use of the Lemur Toolkit for Language Modeling and Information Retrieval is subject to the terms
 * of the software license set forth in the LICENSE file included with this software, and also
 * available at http://www.lemurproject.org/license.html
 *
 * ================================================================================================
 */
package org.lemurproject.lucindri.searcher.solr;

import org.apache.lucene.search.similarities.Similarity;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.schema.SimilarityFactory;
import org.lemurproject.lucindri.searcher.similarities.CoordinateMatchSimilarity;

public class CoordinateMatchSimilarityFactory extends SimilarityFactory {

	@Override
	public void init(SolrParams params) {
		super.init(params);
	}

	@Override
	public Similarity getSimilarity() {
		CoordinateMatchSimilarity sim = new CoordinateMatchSimilarity();
		return sim;
	}

}
