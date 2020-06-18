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
import java.util.Collection;

import org.apache.lucene.search.ConjunctionDISI;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.search.Weight;

public class IndriConjunctionScorer extends Scorer {

	final DocIdSetIterator disi;
	final Scorer[] scorers;
	final Collection<Scorer> required;

	protected IndriConjunctionScorer(Weight weight, Collection<Scorer> required, Collection<Scorer> scorers)
			throws IOException {
		super(weight);
		assert required.containsAll(scorers);
		this.disi = ConjunctionDISI.intersectScorers(required);
		this.scorers = scorers.toArray(new Scorer[scorers.size()]);
		this.required = required;
	}

	@Override
	public TwoPhaseIterator twoPhaseIterator() {
		return TwoPhaseIterator.unwrap(disi);
	}

	@Override
	public DocIdSetIterator iterator() {
		return disi;
	}

	@Override
	public int docID() {
		return disi.docID();
	}

	@Override
	public float score() throws IOException {
		double sum = 0.0d;
		for (Scorer scorer : scorers) {
			sum += scorer.score();
		}
		return (float) sum;
	}

	@Override
	public float getMaxScore(int upTo) throws IOException {
		return 0;
	}

	@Override
	public int advanceShallow(int target) throws IOException {
		if (scorers.length == 1) {
			return scorers[0].advanceShallow(target);
		}
		return super.advanceShallow(target);
	}

	@Override
	public void setMinCompetitiveScore(float minScore) throws IOException {
		// This scorer is only used for TOP_SCORES when there is a single scoring clause
		if (scorers.length == 1) {
			scorers[0].setMinCompetitiveScore(minScore);
		}
	}

	@Override
	public Collection<ChildScorable> getChildren() {
		ArrayList<ChildScorable> children = new ArrayList<>();
		for (Scorer scorer : required) {
			children.add(new ChildScorable(scorer, "MUST"));
		}
		return children;
	}

	static final class DocsAndFreqs {
		final long cost;
		final DocIdSetIterator iterator;
		int doc = -1;

		DocsAndFreqs(DocIdSetIterator iterator) {
			this.iterator = iterator;
			this.cost = iterator.cost();
		}
	}

}
