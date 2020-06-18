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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BulkScorer;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.Similarity;

public class IndriWeightedSum extends IndriWeight {
	/** The Similarity implementation. */
	private final Similarity similarity;
	private final IndriWeightedSumQuery query;

	private final ArrayList<Weight> weights;
	private final ScoreMode scoreMode;
	private final float boost;

	public IndriWeightedSum(IndriWeightedSumQuery query, IndexSearcher searcher, ScoreMode scoreMode, float boost)
			throws IOException {
		super(query, searcher, scoreMode, boost);
		this.query = query;
		this.boost = boost;
		this.scoreMode = scoreMode;
		this.similarity = searcher.getSimilarity();
		weights = new ArrayList<>();
		for (BooleanClause c : query) {
			Weight w = searcher.createWeight(c.getQuery(), scoreMode, 1.0f);
			weights.add(w);
		}
	}

	private Scorer getScorer(LeafReaderContext context) throws IOException {
		List<Scorer> subScorers = new ArrayList<>();
		Iterator<BooleanClause> cIter = query.iterator();
		for (Weight w : weights) {
			BooleanClause c = cIter.next();
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
			scorer = new IndriWeightedSumScorer(this, subScorers, scoreMode, boost);
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
