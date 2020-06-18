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
import java.util.Collections;
import java.util.List;

import org.apache.lucene.search.DisiWrapper;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

public class IndriMaxScorer extends IndriDisjunctionScorer implements SmoothingScorer {

	protected IndriMaxScorer(Weight weight, List<Scorer> subScorers, ScoreMode scoreMode, float boost)
			throws IOException {
		super(weight, subScorers, scoreMode, boost);
	}

	@Override
	protected float score(DisiWrapper topList) throws IOException {
		List<Float> scoreArray = new ArrayList<>();
		for (DisiWrapper w = topList; w != null; w = w.next) {
			int docId = this.docID();
			int scorerDocId = w.scorer.docID();
			if (docId == scorerDocId) {
				scoreArray.add(w.scorer.score());
			} else if (w.scorer instanceof SmoothingScorer) {
				float smoothingScore = ((SmoothingScorer) w.scorer).smoothingScore(w, docId);
				scoreArray.add(smoothingScore);
			}
		}
		float score = Collections.max(scoreArray);
		return (float) (score);
	}

	@Override
	public float smoothingScore(DisiWrapper topList, int docId) throws IOException {
		DisiWrapper test = getSubMatches();
		List<Float> scoreArray = new ArrayList<>();
		for (DisiWrapper w = test; w != null; w = w.next) {
			int scorerDocId = w.scorer.docID();
			if (docId == scorerDocId) {
				scoreArray.add(w.scorer.score());
			} else if (w.scorer instanceof SmoothingScorer) {
				float smoothingScore = ((SmoothingScorer) w.scorer).smoothingScore(w, docId);
				scoreArray.add(smoothingScore);
			}
		}
		float score = Collections.max(scoreArray);
		return (float) (score);

	}

}
