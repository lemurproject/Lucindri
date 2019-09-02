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

import java.util.List;
import java.util.Locale;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.similarities.IndriSimilarity.IndriStats;

/**
 */
public class IndriJelinekMercerSimilarity extends LMSimilarity {
	/** The &lambda; parameter. */
	private final float lambda;

	/** Instantiates with the specified collectionModel and &lambda; parameter. */
	public IndriJelinekMercerSimilarity(CollectionModel collectionModel, float lambda) {
		super(collectionModel);
		this.lambda = lambda;
	}

	/** Instantiates with the specified &lambda; parameter. */
	public IndriJelinekMercerSimilarity(float lambda) {
		super(new IndriCollectionModel());
		this.lambda = lambda;
	}

	public IndriJelinekMercerSimilarity() {
		this(.4f);
	}

	@Override
	protected double score(BasicStats stats, double freq, double docLen) {
		return (double) (Math
				.log(((1 - lambda) * freq / docLen) + (lambda * ((IndriStats) stats).getCollectionProbability())));
	}

	@Override
	protected void explain(List<Explanation> subs, BasicStats stats, double freq, double docLen) {
		if (stats.getBoost() != 1.0f) {
			subs.add(Explanation.match(stats.getBoost(), "boost"));
		}
		Explanation weightExpl = Explanation.match(
				(float) Math.log(
						((1 - lambda) * freq / docLen) + (lambda * ((IndriStats) stats).getCollectionProbability())),
				"term weight");
		subs.add(weightExpl);
		subs.add(Explanation.match(lambda, "lambda"));
		super.explain(subs, stats, freq, docLen);
	}

	/** Returns the &lambda; parameter. */
	public float getLambda() {
		return lambda;
	}

	@Override
	public String getName() {
		return String.format(Locale.ROOT, "IndriJelinek-Mercer(%f)", getLambda());
	}

	/**
	 * Models {@code p(w|C)} as the number of occurrences of the term in the
	 * collection, divided by the total number of tokens {@code + 1}.
	 */
	public static class IndriCollectionModel implements CollectionModel {

		/** Sole constructor: parameter-free */
		public IndriCollectionModel() {
		}

		@Override
		public double computeProbability(BasicStats stats) {
			return ((float) stats.getTotalTermFreq()) / ((float) stats.getNumberOfFieldTokens());
		}

		@Override
		public String getName() {
			return null;
		}
	}
}
