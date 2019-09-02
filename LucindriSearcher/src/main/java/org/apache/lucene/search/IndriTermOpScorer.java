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

public class IndriTermOpScorer extends Scorer implements SmoothingScorer, WeightedScorer {

	private final IndriTermOpEnum postingsEnum;
	private final LeafSimScorer docScorer;

	protected IndriTermOpScorer(Weight weight, IndriTermOpEnum postingsEnum, LeafSimScorer docScorer) {
		super(weight);
		this.docScorer = docScorer;
		this.postingsEnum = postingsEnum;
	}

	@Override
	public int docID() {
		return postingsEnum.docID();
	}

	final int freq() throws IOException {
		return postingsEnum.freq();
	}

	@Override
	public DocIdSetIterator iterator() {
		return postingsEnum;
	}

	@Override
	public float score() throws IOException {
		assert docID() != DocIdSetIterator.NO_MORE_DOCS;
		return docScorer.score(postingsEnum.docID(), postingsEnum.freq());
	}

	/** Returns a string representation of this <code>TermScorer</code>. */
	@Override
	public String toString() {
		return "scorer(" + weight + ")[" + super.toString() + "]";
	}

	@Override
	public float getBoost() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float smoothingScore(DisiWrapper topList, int docId) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getMaxScore(int upTo) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

}
