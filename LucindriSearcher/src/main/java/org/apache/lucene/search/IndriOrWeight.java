/*
 * ===============================================================================================
 * Copyright (c) 2019 Carnegie Mellon University and University of Massachusetts. All Rights
 * Reserved.
 *
 * Use of the Lemur Toolkit for Language Modeling and Information Retrieval is subject to the terms
 * of the software license set forth in the LICENSE file included with this software, and also
 * available at http://www.lemurproject.org/license.html
 *
 * ================================================================================================
 */
package org.apache.lucene.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.LeafReaderContext;
import org.lemurproject.lucindri.searcher.IndriOrQuery;

public class IndriOrWeight extends IndriWeight {

	private final ArrayList<Weight> weights;
	private final ScoreMode scoreMode;

	public IndriOrWeight(IndriOrQuery query, IndexSearcher searcher, ScoreMode scoreMode, float boost)
			throws IOException {
		super(query, searcher, scoreMode, boost);
		this.scoreMode = scoreMode;
		weights = new ArrayList<>();
		for (BooleanClause c : query) {
			Weight w = searcher.createWeight(c.getQuery(), scoreMode, boost);
			weights.add(w);
		}
	}

	private Scorer getScorer(LeafReaderContext context) throws IOException {
		List<Scorer> subScorers = new ArrayList<>();
		for (Weight w : weights) {
			Scorer scorer = w.scorer(context);
			if (scorer != null) {
				subScorers.add(scorer);
			}
		}

		if (subScorers.isEmpty()) {
			return null;
		}

		Scorer scorer = subScorers.get(0);
		if (subScorers.size() > 1) {
			scorer = new IndriOrScorer(this, subScorers, scoreMode);
		}
		return scorer;
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
