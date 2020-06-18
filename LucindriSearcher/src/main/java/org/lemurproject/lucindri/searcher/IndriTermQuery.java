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
import java.util.Objects;
import java.util.Set;

import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermState;
import org.apache.lucene.index.TermStates;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafSimScorer;
import org.apache.lucene.search.Matches;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.Similarity;

public class IndriTermQuery extends Query {

	private final Term term;
	private final TermStates perReaderTermState;

	final class IndriTermWeight extends Weight {
		private final Similarity similarity;
		private final float boost;
		private final Similarity.SimScorer simScorer;
		private final TermStates termStates;
		private final ScoreMode scoreMode;

		public IndriTermWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost, TermStates termStates)
				throws IOException {
			super(IndriTermQuery.this);
			this.boost = boost;
			this.similarity = searcher.getSimilarity();
			this.scoreMode = ScoreMode.TOP_SCORES;
			this.termStates = termStates;

			final CollectionStatistics collectionStats;
			final TermStatistics termStats;
			collectionStats = searcher.collectionStatistics(IndriTermQuery.this.getTerm().field());
			termStats = searcher.termStatistics(IndriTermQuery.this.getTerm(), termStates);

			if (termStats == null) {
				this.simScorer = null; // term doesn't exist in any segment, we won't use similarity at all
			} else {
				this.simScorer = similarity.scorer(boost, collectionStats, termStats);
			}
		}

		@Override
		public void extractTerms(Set<Term> terms) {
			terms.add(getTerm());
		}

		@Override
		public Matches matches(LeafReaderContext context, int doc) throws IOException {
			return null;
		}

		@Override
		public String toString() {
			return "weight(" + IndriTermQuery.this + ")";
		}

		@Override
		public Scorer scorer(LeafReaderContext context) throws IOException {
			assert termStates == null || termStates.wasBuiltFor(ReaderUtil.getTopLevelContext(
					context)) : "The top-reader used to create Weight is not the same as the current reader's top-reader ("
							+ ReaderUtil.getTopLevelContext(context);
			;
			final TermsEnum termsEnum = getTermsEnum(context);
			if (termsEnum == null || simScorer == null) {
				return null;
			}
			LeafSimScorer scorer = new LeafSimScorer(simScorer, context.reader(), term.field(),
					scoreMode.needsScores());

			Scorer indriTermScorer = new IndriTermScorer(this, termsEnum.impacts(PostingsEnum.POSITIONS), scorer,
					this.boost);
			return indriTermScorer;
		}

		/**
		 * Returns a {@link TermsEnum} positioned at this weights Term or null if the
		 * term does not exist in the given context
		 */
		private TermsEnum getTermsEnum(LeafReaderContext context) throws IOException {
			assert termStates != null;
			assert termStates.wasBuiltFor(ReaderUtil.getTopLevelContext(
					context)) : "The top-reader used to create Weight is not the same as the current reader's top-reader ("
							+ ReaderUtil.getTopLevelContext(context);
			final TermState state = termStates.get(context);
			if (state == null) { // term is not present in that reader
				assert termNotInReader(context.reader(), term) : "no termstate found but term exists in reader term="
						+ term;
				return null;
			}
			final TermsEnum termsEnum = context.reader().terms(term.field()).iterator();
			termsEnum.seekExact(term.bytes(), state);
			return termsEnum;
		}

		private boolean termNotInReader(LeafReader reader, Term term) throws IOException {
			return reader.docFreq(term) == 0;
		}

		@Override
		public boolean isCacheable(LeafReaderContext ctx) {
			return true;
		}

		@Override
		public Explanation explain(LeafReaderContext context, int doc) throws IOException {
			IndriTermScorer scorer = (IndriTermScorer) scorer(context);
			if (scorer != null) {
				int newDoc = scorer.iterator().advance(doc);
				if (newDoc == doc) {
					float freq = scorer.freq();
					LeafSimScorer docScorer = new LeafSimScorer(simScorer, context.reader(), term.field(), true);
					Explanation freqExplanation = Explanation.match(freq, "freq, occurrences of term within document");
					Explanation scoreExplanation = docScorer.explain(doc, freqExplanation);
					return Explanation.match(scoreExplanation.getValue(), "weight(" + getQuery() + " in " + doc + ") ["
							+ similarity.getClass().getSimpleName() + "], result of:", scoreExplanation);
				}
			}
			return Explanation.noMatch("no matching term");
		}
	}

	/** Constructs a query for the term <code>t</code>. */
	public IndriTermQuery(Term t) {
		this.term = t;
		this.perReaderTermState = null;
	}

	/**
	 * Expert: constructs a TermQuery that will use the provided docFreq instead of
	 * looking up the docFreq against the searcher.
	 */
	public IndriTermQuery(Term t, TermStates states) {
		this.term = t;
		this.perReaderTermState = Objects.requireNonNull(states);
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) throws IOException {
		final IndexReaderContext context = searcher.getTopReaderContext();
		final TermStates termState;
		if (perReaderTermState == null || perReaderTermState.wasBuiltFor(context) == false) {
			termState = TermStates.build(context, term, scoreMode.needsScores());
		} else {
			// PRTS was pre-build for this IS
			termState = this.perReaderTermState;
		}

		return new IndriTermWeight(searcher, scoreMode, boost, termState);
	}

	/** Returns the term of this query. */
	public Term getTerm() {
		return term;
	}

	/**
	 * Returns the {@link TermStates} passed to the constructor, or null if it was
	 * not passed.
	 *
	 * @lucene.experimental
	 */
	public TermStates getTermStates() {
		return perReaderTermState;
	}

	@Override
	public String toString(String field) {
		StringBuilder buffer = new StringBuilder();
		if (!term.field().equals(field)) {
			buffer.append(term.field());
			buffer.append(":");
		}
		buffer.append(term.text());
		return buffer.toString();
	}

	@Override
	public boolean equals(Object other) {
		return sameClassAs(other) && term.equals(((IndriTermQuery) other).term);
	}

	@Override
	public int hashCode() {
		return classHash() ^ term.hashCode();
	}

}
