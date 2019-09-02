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
import java.util.List;

public class IndriAndScorer extends DisjunctionScorer implements SmoothingScorer, WeightedScorer {
	private DisiWrapper subAvgScorers;
	private float boost;

	protected IndriAndScorer(Weight weight, List<Scorer> subScorers, ScoreMode scoreMode, float boost)
			throws IOException {
		super(weight, subScorers, scoreMode);
		this.boost = boost;
		this.subAvgScorers = null;
		DisiWrapper prevWrapper = null;
		for (Scorer scorer : subScorers) {
			final DisiWrapper w = new DisiWrapper(scorer);
			if (subAvgScorers == null) {
				subAvgScorers = w;
			} else {
				prevWrapper.next = w;
			}
			prevWrapper = w;
		}
	}

	@Override
	protected float score(DisiWrapper topList) throws IOException {
		double score = 0;
		double boostSum = 0.0;
		for (DisiWrapper w = topList; w != null; w = w.next) {
			int docId = this.docID();
			int scorerDocId = w.scorer.docID();
			if (docId == scorerDocId) {
				double tempScore = w.scorer.score();
				if (w.scorer instanceof WeightedScorer) {
					tempScore *= ((WeightedScorer) w.scorer).getBoost();
				}
				score += tempScore;
			} else if (w.scorer instanceof SmoothingScorer) {
				float smoothingScore = ((SmoothingScorer) w.scorer).smoothingScore(w, docId);
				if (w.scorer instanceof WeightedScorer) {
					smoothingScore *= ((WeightedScorer) w.scorer).getBoost();
				}
				score += smoothingScore;
			}
			if (w.scorer instanceof WeightedScorer) {
				boostSum += ((WeightedScorer) w.scorer).getBoost();
			} else {
				boostSum++;
			}
		}
		return (float) (score / boostSum);
	}

	@Override
	public float smoothingScore(DisiWrapper topList, int docId) throws IOException {
		DisiWrapper test = getSubMatches();
		double score = 0;
		double boostSum = 0.0;
		for (DisiWrapper w = test; w != null; w = w.next) {
			int scorerDocId = w.scorer.docID();
			float boost = 1.0f;
			if (w.scorer instanceof WeightedScorer) {
				boost = ((WeightedScorer) w.scorer).getBoost();
			}
			boostSum += boost;
			if (docId == scorerDocId) {
				score += boost * w.scorer.score();
			} else if (w.scorer instanceof SmoothingScorer) {
				float smoothingScore = boost * ((SmoothingScorer) w.scorer).smoothingScore(w, docId);
				score += smoothingScore;
			}
		}
		return (float) (score / boostSum);

	}

	@Override
	DisiWrapper getSubMatches() throws IOException {
		return subAvgScorers;
	}

	@Override
	public float getBoost() {
		return this.boost;
	}

	@Override
	public float getMaxScore(int upTo) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

}
