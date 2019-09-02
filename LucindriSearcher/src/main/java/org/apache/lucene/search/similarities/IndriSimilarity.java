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
package org.apache.lucene.search.similarities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.util.SmallFloat;

public abstract class IndriSimilarity extends Similarity {
	/** For {@link #log2(double)}. Precomputed for efficiency reasons. */
	private static final double LOG_2 = Math.log(2);

	/**
	 * True if overlap tokens (tokens with a position of increment of zero) are
	 * discounted from the document's length.
	 */
	protected boolean discountOverlaps = true;

	/** The collection model. */
	protected final CollectionModel collectionModel;

	private Map<String, Long> fieldDocLenTotalMap;

	private int norm = -1;

	/** Creates a new instance with the specified collection language model. */
	public IndriSimilarity(CollectionModel collectionModel) {
		this.collectionModel = collectionModel;
		this.fieldDocLenTotalMap = new HashMap<String, Long>();
	}

	/** Creates a new instance with the default collection language model. */
	public IndriSimilarity() {
		this(new DefaultCollectionModel());
	}

	protected BasicStats newStats(String field, double boost) {
		return new IndriStats(field, boost);
	}

	/**
	 * Computes the collection probability of the current term in addition to the
	 * usual statistics.
	 */
	protected void fillBasicStats(BasicStats stats, CollectionStatistics collectionStats, TermStatistics termStats) {
		// TODO: add sumDocFreq for field (numberOfFieldPostings)
		stats.setNumberOfDocuments(collectionStats.docCount());
		stats.setNumberOfFieldTokens(collectionStats.sumTotalTermFreq());
		stats.setAvgFieldLength(collectionStats.sumTotalTermFreq() / (double) collectionStats.docCount());
		stats.setDocFreq(termStats.docFreq());
		stats.setTotalTermFreq(termStats.totalTermFreq());

		IndriStats indriStats = (IndriStats) stats;
		indriStats.setCollectionProbability(collectionModel.computeProbability(stats));
	}

	protected void explain(List<Explanation> subExpls, BasicStats stats, double freq, double docLen) {
		subExpls.add(Explanation.match((float) collectionModel.computeProbability(stats), "collection probability"));
	}

	/**
	 * Determines whether overlap tokens (Tokens with 0 position increment) are
	 * ignored when computing norm. By default this is true, meaning overlap tokens
	 * do not count when computing norms.
	 *
	 * @lucene.experimental
	 *
	 * @see #computeNorm
	 */
	public void setDiscountOverlaps(boolean v) {
		discountOverlaps = v;
	}

	/**
	 * Returns true if overlap tokens are discounted from the document's length.
	 * 
	 * @see #setDiscountOverlaps
	 */
	public boolean getDiscountOverlaps() {
		return discountOverlaps;
	}

	@Override
	public final SimScorer scorer(float boost, CollectionStatistics collectionStats, TermStatistics... termStats) {
		SimScorer weights[] = new SimScorer[termStats.length];
		for (int i = 0; i < termStats.length; i++) {
			BasicStats stats = newStats(collectionStats.field(), boost);
			fillBasicStats(stats, collectionStats, termStats[i]);
			weights[i] = new BasicSimScorer(stats);
		}
		if (weights.length == 1) {
			return weights[0];
		} else {
			return new MultiSimilarity.MultiSimScorer(weights);
		}
	}

	/**
	 * Scores the document {@code doc}.
	 * <p>
	 * Subclasses must apply their scoring formula in this class.
	 * </p>
	 * 
	 * @param stats  the corpus level statistics.
	 * @param freq   the term frequency.
	 * @param docLen the document length.
	 * @return the score.
	 */
	protected abstract double score(BasicStats stats, double freq, double docLen);

	/**
	 * Explains the score. The implementation here provides a basic explanation in
	 * the format <em>score(name-of-similarity, doc=doc-id, freq=term-frequency),
	 * computed from:</em>, and attaches the score (computed via the
	 * {@link #score(BasicStats, double, double)} method) and the explanation for
	 * the term frequency. Subclasses content with this format may add additional
	 * details in {@link #explain(List, BasicStats, double, double)}.
	 * 
	 * @param stats  the corpus level statistics.
	 * @param freq   the term frequency and its explanation.
	 * @param docLen the document length.
	 * @return the explanation.
	 */
	protected Explanation explain(BasicStats stats, Explanation freq, double docLen) {
		List<Explanation> subs = new ArrayList<>();
		explain(subs, stats, freq.getValue().floatValue(), docLen);

		return Explanation.match((float) score(stats, freq.getValue().floatValue(), docLen),
				"score(" + getClass().getSimpleName() + ", freq=" + freq.getValue() + "), computed from:", subs);
	}

	// ------------------------------ Norm handling ------------------------------

	/** Cache of decoded bytes. */
	private static final float[] LENGTH_TABLE = new float[256];

	static {
		for (int i = 0; i < 256; i++) {
			LENGTH_TABLE[i] = SmallFloat.byte4ToInt((byte) i);
		}
	}

	public final void setNorm(int value) {
		this.norm = value;
	}

	/**
	 * Encodes the document length as the total number of tokens including
	 * stopwords.
	 */
	@Override
	public final long computeNorm(FieldInvertState state) {
		long numTerms = 0;
		if (state.getIndexOptions() == IndexOptions.DOCS && state.getIndexCreatedVersionMajor() >= 8) {
			numTerms = state.getUniqueTermCount();
		} else if (discountOverlaps) {
			numTerms = state.getPosition() - state.getNumOverlap();
		} else if (state.getPosition() > 0) {
			numTerms = state.getPosition();
		}
		return numTerms + 1;

	}

	public Map<String, Long> getTotalFieldLengths() {
		return fieldDocLenTotalMap;
	}

	// ----------------------------- Static methods ------------------------------

	/** Returns the base two logarithm of {@code x}. */
	public static double log2(double x) {
		// Put this to a 'util' class if we need more of these.
		return Math.log(x) / LOG_2;
	}

	// --------------------------------- Classes ---------------------------------

	/**
	 * Delegates the {@link #score(float, long)} and
	 * {@link #explain(Explanation, long)} methods to
	 * {@link SimilarityBase#score(BasicStats, double, double)} and
	 * {@link SimilarityBase#explain(BasicStats, Explanation, double)},
	 * respectively.
	 */
	final class BasicSimScorer extends SimScorer {
		final BasicStats stats;

		BasicSimScorer(BasicStats stats) {
			this.stats = stats;
		}

		double getLengthValue(long norm) {
			return LENGTH_TABLE[Byte.toUnsignedInt((byte) norm)];
			// return LENGTH_TABLE[(int) norm];
		}

		@Override
		public float score(float freq, long norm) {
			return (float) IndriSimilarity.this.score(stats, freq, getLengthValue(norm));
		}

		@Override
		public Explanation explain(Explanation freq, long norm) {
			return IndriSimilarity.this.explain(stats, freq, getLengthValue(norm));
		}

	}

	/** Stores the collection distribution of the current term. */
	public static class IndriStats extends BasicStats {
		/** The probability that the current term is generated by the collection. */
		private double collectionProbability;

		/**
		 * Creates LMStats for the provided field and query-time boost
		 */
		public IndriStats(String field, double boost) {
			super(field, boost);
		}

		/**
		 * Returns the probability that the current term is generated by the collection.
		 */
		public final double getCollectionProbability() {
			return collectionProbability;
		}

		/**
		 * Sets the probability that the current term is generated by the collection.
		 */
		public final void setCollectionProbability(double collectionProbability) {
			this.collectionProbability = collectionProbability;
		}
	}

	/** A strategy for computing the collection language model. */
	public static interface CollectionModel {
		/**
		 * Computes the probability {@code p(w|C)} according to the language model
		 * strategy for the current term.
		 */
		public double computeProbability(BasicStats stats);

		/** The name of the collection model strategy. */
		public String getName();
	}

	/**
	 * Models {@code p(w|C)} as the number of occurrences of the term in the
	 * collection, divided by the total number of tokens {@code + 1}.
	 */
	public static class DefaultCollectionModel implements CollectionModel {

		/** Sole constructor: parameter-free */
		public DefaultCollectionModel() {
		}

		@Override
		public double computeProbability(BasicStats stats) {
			return ((double) stats.getTotalTermFreq()) / ((double) stats.getNumberOfFieldTokens());
		}

		@Override
		public String getName() {
			return null;
		}
	}
}
