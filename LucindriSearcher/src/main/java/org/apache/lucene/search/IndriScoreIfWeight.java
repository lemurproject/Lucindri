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
import java.util.Set;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;

public class IndriScoreIfWeight extends Weight {

	private final Weight requiredWeight;
	private final Weight queryWeight;
	private final ScoreMode scoreMode;

	public IndriScoreIfWeight(Query required, Query query, IndexSearcher searcher, ScoreMode scoreMode, float boost)
			throws IOException {
		super(query);
		this.requiredWeight = required.createWeight(searcher, scoreMode, boost);
		this.queryWeight = query.createWeight(searcher, scoreMode, boost);
		this.scoreMode = scoreMode;
	}

	@Override
	public boolean isCacheable(LeafReaderContext ctx) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void extractTerms(Set<Term> terms) {
		// TODO Auto-generated method stub

	}

	@Override
	public Explanation explain(LeafReaderContext context, int doc) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	private Scorer getScorer(LeafReaderContext context) throws IOException {
		Scorer scorer = new IndriScoreIfScorer(queryWeight.scorer(context), requiredWeight.scorer(context));
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
