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

import org.apache.lucene.search.DisiPriorityQueue;
import org.apache.lucene.search.DisiWrapper;
import org.apache.lucene.search.DisjunctionDISIApproximation;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

abstract public class IndriDisjunctionScorer extends Scorer implements WeightedScorer {

	private float boost;
	private DisiWrapper subAvgScorers;
	private final DisiPriorityQueue subScorers;
	private final DocIdSetIterator approximation;

	protected IndriDisjunctionScorer(Weight weight, List<Scorer> subScorers, ScoreMode scoreMode, float boost) {
		super(weight);
		this.boost = boost;
		this.subScorers = new DisiPriorityQueue(subScorers.size());
		for (Scorer scorer : subScorers) {
			final DisiWrapper w = new DisiWrapper(scorer);
			this.subScorers.add(w);
		}
		this.approximation = new DisjunctionDISIApproximation(this.subScorers);
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
	public DocIdSetIterator iterator() {
		return approximation;
	}

	@Override
	public float getMaxScore(int upTo) throws IOException {
		return 0;
	}

	DisiWrapper getSubMatches() throws IOException {
		return subAvgScorers;
	}

	abstract float score(DisiWrapper topList) throws IOException;

	@Override
	public float score() throws IOException {
		return score(getSubMatches());
	}

	@Override
	public int docID() {
		return subScorers.top().doc;
	}

	@Override
	public float getBoost() {
		return this.boost;
	}

}
