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

public class IndriOrScorer extends IndriDisjunctionScorer implements SmoothingScorer {

	protected IndriOrScorer(Weight weight, List<Scorer> subScorers, ScoreMode scoreMode, float boost)
			throws IOException {
		super(weight, subScorers, scoreMode, boost);
	}

	@Override
	protected float score(DisiWrapper topList) throws IOException {
		double score = 1;
		for (DisiWrapper w = topList; w != null; w = w.next) {
			int docId = this.docID();
			int scorerDocId = w.scorer.docID();
			if (docId == scorerDocId) {
				score *= (1 - Math.exp(w.scorer.score()));
			} else if (w.scorer instanceof SmoothingScorer) {
				float smoothingScore = ((SmoothingScorer) w.scorer).smoothingScore(topList, docId);
				score *= (1 - Math.exp(smoothingScore));
			}
		}
		return (float) (Math.log(1.0 - score));
	}

	@Override
	public float smoothingScore(DisiWrapper topList, int docId) throws IOException {
		double score = 1;
		DisiWrapper test = getSubMatches();
		for (DisiWrapper w = test; w != null; w = w.next) {
			int scorerDocId = w.scorer.docID();
			if (docId == scorerDocId) {
				score *= (1 - Math.exp(w.scorer.score()));
			} else if (w.scorer instanceof SmoothingScorer) {
				float smoothingScore = ((SmoothingScorer) w.scorer).smoothingScore(w, docId);
				score *= (1 - Math.exp(smoothingScore));
			}
		}
		return (float) (Math.log(1.0 - score));
	}

}
