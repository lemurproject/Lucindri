package org.lemurproject.lucindri.searcher;

import java.io.IOException;

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

abstract public class IndriScorer extends Scorer {

	private float boost;

	protected IndriScorer(Weight weight, float boost) {
		super(weight);
		if (boost != 0) {
			this.boost = boost;
		} else {
			this.boost = 1f;
		}
	}

	@Override
	abstract public DocIdSetIterator iterator();

	@Override
	abstract public float getMaxScore(int upTo) throws IOException;

	@Override
	abstract public float score() throws IOException;

	abstract public float smoothingScore(int docId) throws IOException;

	@Override
	abstract public int docID();

	public float getBoost() {
		return this.boost;
	}

}
