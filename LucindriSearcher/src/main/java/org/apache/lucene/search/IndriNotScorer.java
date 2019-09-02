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

public class IndriNotScorer extends Scorer implements SmoothingScorer {
	private Scorer subScorer;

	protected IndriNotScorer(Weight weight, Scorer subScorer) {
		super(weight);
		this.subScorer = subScorer;
	}

	@Override
	public float score() throws IOException {
		float score = subScorer.score();
		if (subScorer instanceof WeightedScorer) {
			score *= ((WeightedScorer) subScorer).getBoost();
		}
		return (float) (Math.log(1.0 - Math.exp(score)));
	}

	@Override
	public float smoothingScore(DisiWrapper topList, int docId) throws IOException {
		float score = 0.0f;
		if (subScorer instanceof SmoothingScorer) {
			score = ((SmoothingScorer) subScorer).smoothingScore(null, docId);
		}
		if (subScorer instanceof WeightedScorer) {
			score *= ((WeightedScorer) subScorer).getBoost();
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
		// TODO Auto-generated method stub
		return 0;
	}

}
