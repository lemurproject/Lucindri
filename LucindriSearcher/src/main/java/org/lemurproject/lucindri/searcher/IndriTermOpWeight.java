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
import java.util.List;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BulkScorer;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafSimScorer;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.Similarity;

public abstract class IndriTermOpWeight extends IndriWeight {

	private static ScoreMode scoreMode = ScoreMode.COMPLETE;
	private final ArrayList<Weight> weights;
	private final String field;
	private final float boost;
	private final Similarity similarity;
	private CollectionStatistics collectionStats;
	private Similarity.SimScorer simScorer;

	protected IndriTermOpWeight(IndriProximityQuery query, IndexSearcher searcher, String field, float boost)
			throws IOException {
		super(query, searcher, scoreMode, boost);
		this.field = field;
		this.boost = boost;
		this.similarity = searcher.getSimilarity();
		collectionStats = searcher.collectionStatistics(field);
		weights = new ArrayList<>();
		for (BooleanClause c : query) {
			Weight w = searcher.createWeight(c.getQuery(), scoreMode, 1.0f);
			weights.add(w);
		}
	}

	protected Scorer getScorer(LeafReaderContext context) throws IOException {
		List<IndriDocAndPostingsIterator> iterators = new ArrayList<>();
		for (Weight w : weights) {
			Scorer scorer = w.scorer(context);
			if (scorer != null) {
				IndriDocAndPostingsIterator iterator = null;
				if (scorer.iterator() instanceof IndriTermOpEnum) {
					iterator = ((IndriTermOpEnum) scorer.iterator());
				} else if (scorer.iterator() instanceof PostingsEnum) {
					iterator = new IndriPostingsEnumWrapper((PostingsEnum) scorer.iterator());
				}
				iterators.add(iterator);
			}
		}

		if (iterators.isEmpty()) {
			return null;
		}

		IndriTermOpEnum postingsEnum = getProximityIterator(iterators);
		TermStatistics termStats = postingsEnum.getInvList().getTermStatistics();
		Scorer scorer = null;
		if (termStats != null) {
			this.simScorer = similarity.scorer(boost, collectionStats, termStats);
			LeafSimScorer leafScorer = new LeafSimScorer(simScorer, context.reader(), field, true);
			scorer = new IndriTermOpScorer(this, postingsEnum, leafScorer, boost);
		}
		return scorer;
	}

	protected IndriTermOpEnum getProximityIterator(List<IndriDocAndPostingsIterator> iterators) throws IOException {
		IndriInvertedList invList = createInvertedList(iterators);
		IndriTermOpEnum nearPostings = new IndriTermOpEnum(invList);

		return nearPostings;
	}

	protected abstract IndriInvertedList createInvertedList(List<IndriDocAndPostingsIterator> iterators)
			throws IOException;

	public String getField() {
		return field;
	}

	@Override
	public Scorer scorer(LeafReaderContext context) throws IOException {
		return getScorer(context);
	}

	@Override
	public BulkScorer bulkScorer(LeafReaderContext context) throws IOException {
		Scorer scorer = getScorer(context);
		if (scorer != null) {
			BulkScorer bulkScorer = new DefaultBulkScorer(scorer);
			return bulkScorer;
		}
		return null;
	}

}
