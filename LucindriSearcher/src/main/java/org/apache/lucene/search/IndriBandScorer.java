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
import java.util.Collections;
import java.util.List;

public class IndriBandScorer extends ConjunctionScorer implements WeightedScorer {
	private float boost;

	protected IndriBandScorer(Weight weight, List<Scorer> subScorers, ScoreMode scoreMode, float boost)
			throws IOException {
		super(weight, subScorers, subScorers);
	}

	@Override
	public float score() throws IOException {
		double score = 0;
		List<Double> scoreArray = new ArrayList<>();
		for (Scorer scorer : scorers) {
			double tempScore = scorer.score();
			score += tempScore;
			scoreArray.add(tempScore);
		}
		double maxScore = Collections.min(scoreArray);
		return 1.0f;
		// return (float) maxScore;
		// return (float) (score / scorers.length);
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
