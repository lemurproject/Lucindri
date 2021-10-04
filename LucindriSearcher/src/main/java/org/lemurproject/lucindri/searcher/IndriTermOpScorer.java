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

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndriScorer;
import org.apache.lucene.search.LeafSimScorer;
import org.apache.lucene.search.Weight;

public class IndriTermOpScorer extends IndriScorer {

	private final IndriTermOpEnum postingsEnum;
	private final LeafSimScorer docScorer;
	private final float boost;

	protected IndriTermOpScorer(Weight weight, IndriTermOpEnum postingsEnum, LeafSimScorer docScorer, float boost) {
		super(weight, boost);
		this.docScorer = docScorer;
		this.postingsEnum = postingsEnum;
		this.boost = boost;
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
		return boost;
	}

	@Override
	public float getMaxScore(int upTo) throws IOException {
		return 0;
	}

	@Override
	public float smoothingScore(int docId) throws IOException {
		return docScorer.score(docId, 0);
	}

}
