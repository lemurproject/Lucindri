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

public class IndriOrScorer extends DisjunctionScorer implements SmoothingScorer {
	private DisiWrapper subAvgScorers;

	protected IndriOrScorer(Weight weight, List<Scorer> subScorers, ScoreMode scoreMode) throws IOException {
		super(weight, subScorers, scoreMode);
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
	DisiWrapper getSubMatches() throws IOException {
		return subAvgScorers;
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

	@Override
	public float getMaxScore(int upTo) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

}
