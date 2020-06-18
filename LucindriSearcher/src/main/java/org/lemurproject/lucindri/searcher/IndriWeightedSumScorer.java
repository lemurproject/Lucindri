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
import java.util.List;

import org.apache.lucene.search.DisiWrapper;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

public class IndriWeightedSumScorer extends IndriDisjunctionScorer implements SmoothingScorer, WeightedScorer {

	protected IndriWeightedSumScorer(Weight weight, List<Scorer> subScorers, ScoreMode scoreMode, float boost)
			throws IOException {
		super(weight, subScorers, scoreMode, boost);
	}

	@Override
	protected float score(DisiWrapper topList) throws IOException {
		double score = 0;
		double boostSum = 0.0;
		for (DisiWrapper w = topList; w != null; w = w.next) {
			int docId = this.docID();
			int scorerDocId = w.scorer.docID();
			if (docId == scorerDocId) {
				score += ((WeightedScorer) w.scorer).getBoost() * Math.exp(w.scorer.score());
			} else if (w.scorer instanceof SmoothingScorer) {
				double smoothingScore = ((WeightedScorer) w.scorer).getBoost()
						* Math.exp(((SmoothingScorer) w.scorer).smoothingScore(topList, docId));
				score += smoothingScore;
			}
			if (w.scorer instanceof WeightedScorer) {
				boostSum += ((WeightedScorer) w.scorer).getBoost();
			} else {
				boostSum++;
			}
		}
		return (float) (Math.log((score / boostSum)));
	}

	@Override
	public float smoothingScore(DisiWrapper topList, int docId) throws IOException {
		DisiWrapper test = getSubMatches();
		double score = 0;
		double boostSum = 0.0;
		for (DisiWrapper w = test; w != null; w = w.next) {
			int scorerDocId = w.scorer.docID();
			if (docId == scorerDocId) {
				score += ((WeightedScorer) w.scorer).getBoost() * Math.exp(w.scorer.score());
			} else if (w.scorer instanceof SmoothingScorer) {
				double smoothingScore = ((WeightedScorer) w.scorer).getBoost()
						* Math.exp(((SmoothingScorer) w.scorer).smoothingScore(topList, docId));
				score += smoothingScore;
			}
			if (w.scorer instanceof WeightedScorer) {
				boostSum += ((WeightedScorer) w.scorer).getBoost();
			} else {
				boostSum++;
			}
		}
		return (float) (Math.log((score / boostSum)));
	}

}
