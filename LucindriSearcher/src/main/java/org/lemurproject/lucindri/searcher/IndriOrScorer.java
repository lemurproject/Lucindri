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

import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

public class IndriOrScorer extends IndriDisjunctionScorer {

	protected IndriOrScorer(Weight weight, List<Scorer> subScorers, ScoreMode scoreMode, float boost)
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
		double score = 1;
		for (Scorer scorer : subScorers) {
			if (scorer instanceof IndriScorer) {
				IndriScorer indriScorer = (IndriScorer) scorer;
				int scorerDocId = indriScorer.docID();
				if (docId == scorerDocId) {
					score *= (1 - Math.exp(indriScorer.score()));
				} else {
					score *= (1 - Math.exp(indriScorer.smoothingScore(docId)));
				}
			}
		}
		return (float) (Math.log(1.0 - score));
	}

}
