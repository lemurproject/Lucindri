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
import org.apache.lucene.search.similarities.Similarity;

public class IndriTermQueryWrapper extends Query {

	private final Term term;
	protected final TermQuery termQuery;
	private final TermStates perReaderTermState;

	final class IndriTermWeightWrapper extends Weight {
		private final Weight termWeight;
		private final Similarity similarity;
		private final float boost;
		private final Similarity.SimScorer simScorer;
		private final TermStates termStates;
		private final ScoreMode scoreMode;

		public IndriTermWeightWrapper(IndexSearcher searcher, ScoreMode scoreMode, float boost, TermStates termStates)
				throws IOException {
			super(termQuery);
			this.boost = boost;
			this.termWeight = termQuery.createWeight(searcher, scoreMode, boost);
			this.similarity = searcher.getSimilarity();
			this.scoreMode = scoreMode;
			this.termStates = termStates;

			final CollectionStatistics collectionStats;
			final TermStatistics termStats;
			if (scoreMode.needsScores()) {
				collectionStats = searcher.collectionStatistics(termQuery.getTerm().field());
				termStats = searcher.termStatistics(termQuery.getTerm(), termStates);
			} else {
				// we do not need the actual stats, use fake stats with docFreq=maxDoc and
				// ttf=-1
				final int maxDoc = searcher.getIndexReader().maxDoc();
				collectionStats = new CollectionStatistics(termQuery.getTerm().field(), maxDoc, -1, -1, -1);
				termStats = new TermStatistics(termQuery.getTerm().bytes(), maxDoc, -1);
			}

			if (termStats == null) {
				this.simScorer = null; // term doesn't exist in any segment, we won't use similarity at all
			} else {
				this.simScorer = similarity.scorer(boost, collectionStats, termStats);
			}
		}

		@Override
		public void extractTerms(Set<Term> terms) {
			termWeight.extractTerms(terms);
		}

		@Override
		public Matches matches(LeafReaderContext context, int doc) throws IOException {
			return termWeight.matches(context, doc);
		}

		@Override
		public String toString() {
			return termWeight.toString();
		}

		@Override
		public Scorer scorer(LeafReaderContext context) throws IOException {
			assert termStates == null || termStates.wasBuiltFor(ReaderUtil.getTopLevelContext(
					context)) : "The top-reader used to create Weight is not the same as the current reader's top-reader ("
							+ ReaderUtil.getTopLevelContext(context);
			;
			final TermsEnum termsEnum = getTermsEnum(context);
			Scorer termScorer = null;
			if (termsEnum == null || simScorer == null) {
				return null;
			}
			LeafSimScorer scorer = new LeafSimScorer(simScorer, context.reader(), term.field(),
					scoreMode.needsScores());
			if (scoreMode == ScoreMode.TOP_SCORES) {
				termScorer = new TermScorer(this, termsEnum.impacts(PostingsEnum.POSITIONS), scorer);
			} else {
				termScorer = new TermScorer(this,
						termsEnum.postings(null, scoreMode.needsScores() ? PostingsEnum.POSITIONS : PostingsEnum.NONE),
						scorer);
			}

			Scorer indriTermScorer = new IndriTermScorerWrapper(termWeight, scorer, termScorer, this.boost);
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
			return termWeight.isCacheable(ctx);
		}

		@Override
		public Explanation explain(LeafReaderContext context, int doc) throws IOException {
			return this.termWeight.explain(context, doc);
		}
	}

	/** Constructs a query for the term <code>t</code>. */
	public IndriTermQueryWrapper(Term t) {
		this.term = t;
		this.termQuery = new TermQuery(t);
		this.perReaderTermState = null;
	}

	/**
	 * Expert: constructs a TermQuery that will use the provided docFreq instead of
	 * looking up the docFreq against the searcher.
	 */
	public IndriTermQueryWrapper(Term t, TermStates states) {
		this.term = t;
		this.termQuery = new TermQuery(t, states);
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

		return new IndriTermWeightWrapper(searcher, scoreMode, boost, termState);
	}

	/** Returns the term of this query. */
	public Term getTerm() {
		return this.termQuery.getTerm();
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
		return this.termQuery.toString();
	}

	@Override
	public boolean equals(Object obj) {
		return this.termQuery.equals(obj);
	}

	@Override
	public int hashCode() {
		return this.termQuery.hashCode();
	}

}
