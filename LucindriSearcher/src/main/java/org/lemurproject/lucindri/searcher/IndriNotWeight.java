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
package org.lemurproject.lucindri.searcher;

import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BulkScorer;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

public class IndriNotWeight extends IndriWeight {

	private final Weight weight;
	private final float boost;

	public IndriNotWeight(IndriNotQuery query, IndexSearcher searcher, ScoreMode scoreMode, float boost)
			throws IOException {
		super(query, searcher, scoreMode, boost);
		this.boost = boost;
		// Not Query only has one clause
		BooleanClause c = query.iterator().next();
		weight = searcher.createWeight(c.getQuery(), scoreMode, 1.0f);
	}

	private Scorer getScorer(LeafReaderContext context) throws IOException {
		Scorer scorer = weight.scorer(context);
		if (scorer != null) {
			Scorer scorerWrapper = new IndriNotScorer(this, scorer, boost);
			return scorerWrapper;
		}
		return null;
	}

	@Override
	public Scorer scorer(LeafReaderContext context) throws IOException {
		return getScorer(context);
	}

	@Override
	public BulkScorer bulkScorer(LeafReaderContext context) throws IOException {
		Scorer scorer = getScorer(context);
		if (scorer != null) {
			BulkScorer bulkScorer = new DefaultBulkScorer(scorer);
			return bulkScorer;
		}
		return null;
	}

}
