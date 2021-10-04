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

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndriScorer;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

public class IndriNotScorer extends IndriScorer {
	private Scorer subScorer;

	protected IndriNotScorer(Weight weight, Scorer subScorer, float boost) {
		super(weight, boost);
		this.subScorer = subScorer;
	}

	@Override
	public float score() throws IOException {
		float score = subScorer.score();
		if (subScorer instanceof IndriScorer) {
			score *= ((IndriScorer) subScorer).getBoost();
		}
		return (float) (Math.log(1.0 - Math.exp(score)));
	}

	@Override
	public float smoothingScore(int docId) throws IOException {
		float score = 0.0f;
		score = subScorer.smoothingScore(docId);
		if (subScorer instanceof IndriScorer) {
			score *= ((IndriScorer) subScorer).getBoost();
		}
		return (float) (Math.log(1.0 - Math.exp(score)));

	}

	@Override
	public int docID() {
		return subScorer.docID();
	}

	@Override
	public DocIdSetIterator iterator() {
		return subScorer.iterator();
	}

	@Override
	public float getMaxScore(int upTo) throws IOException {
		return 0;
	}

}
