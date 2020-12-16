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

abstract public class IndriDisjunctionScorer extends IndriScorer {

	private final List<Scorer> subScorersList;
	private final DisiPriorityQueue subScorers;
	private final DocIdSetIterator approximation;

	protected IndriDisjunctionScorer(Weight weight, List<Scorer> subScorersList, ScoreMode scoreMode, float boost) {
		super(weight, boost);
		this.subScorersList = subScorersList;
		this.subScorers = new DisiPriorityQueue(subScorersList.size());
		for (Scorer scorer : subScorersList) {
			final DisiWrapper w = new DisiWrapper(scorer);
			this.subScorers.add(w);
		}
		this.approximation = new DisjunctionDISIApproximation(this.subScorers);
	}

	@Override
	public DocIdSetIterator iterator() {
		return approximation;
	}

	@Override
	public float getMaxScore(int upTo) throws IOException {
		return 0;
	}

	public List<Scorer> getSubMatches() throws IOException {
		return subScorersList;
	}

	abstract float score(List<Scorer> subScorers) throws IOException;

	abstract public float smoothingScore(List<Scorer> subScorers, int docId) throws IOException;

	@Override
	public float score() throws IOException {
		return score(getSubMatches());
	}

	@Override
	public float smoothingScore(int docId) throws IOException {
		return smoothingScore(getSubMatches(), docId);
	}

	@Override
	public int docID() {
		return subScorers.top().doc;
	}

}