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

import org.apache.lucene.search.IndriScorer;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

public class IndriWeightedSumScorer extends IndriDisjunctionScorer {

	protected IndriWeightedSumScorer(Weight weight, List<Scorer> subScorers, ScoreMode scoreMode, float boost)
			throws IOException {
		super(weight, subScorers, scoreMode, boost);
	}

	@Override
	public float score(List<Scorer> subScorers) throws IOException {
		int docId = this.docID();
		return scoreDoc(subScorers, docId);
	}

	@Override
	public float smoothingScore(List<Scorer> subScorers, int docId) throws IOException {
		return scoreDoc(subScorers, docId);
	}

	private float scoreDoc(List<Scorer> subScorers, int docId) throws IOException {
		double score = 0;
		double boostSum = 0.0;
		for (Scorer scorer : subScorers) {
			if (scorer instanceof IndriScorer) {
				IndriScorer indriScorer = (IndriScorer) scorer;
				int scorerDocId = indriScorer.docID();
				double subScore = 0;
				if (docId == scorerDocId) {
					score += indriScorer.getBoost() * Math.exp(indriScorer.score());
				} else {
					double smoothingScore = indriScorer.getBoost() * Math.exp(indriScorer.smoothingScore(docId));
					score += smoothingScore;
				}
				subScore *= indriScorer.getBoost();
				score += subScore;
				boostSum += indriScorer.getBoost();
			}
		}
		if (boostSum == 0) {
			return 0;
		} else {
			return (float) (Math.log((score / boostSum)));
		}
	}

}
