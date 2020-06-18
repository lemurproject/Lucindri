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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

public abstract class IndriWeight extends Weight {

	final IndriQuery query;
	final ArrayList<Weight> weights;
	final ScoreMode scoreMode;

	protected IndriWeight(IndriQuery query, IndexSearcher searcher, ScoreMode scoreMode, float boost)
			throws IOException {
		super(query);
		this.query = query;
		this.scoreMode = scoreMode;
		weights = new ArrayList<>();
		for (BooleanClause c : query) {
			Weight w = searcher.createWeight(c.getQuery(), c.isScoring() ? scoreMode : ScoreMode.COMPLETE_NO_SCORES,
					boost);
			weights.add(w);
		}
	}

	@Override
	public boolean isCacheable(LeafReaderContext ctx) {
		for (Weight w : weights) {
			if (w.isCacheable(ctx) == false)
				return false;
		}
		return true;
	}

	@Override
	public void extractTerms(Set<Term> terms) {
		int i = 0;
		for (BooleanClause clause : query) {
			if (clause.isScoring() || (scoreMode.needsScores() == false && clause.isProhibited() == false)) {
				weights.get(i).extractTerms(terms);
			}
			i++;
		}
	}

	@Override
	public Explanation explain(LeafReaderContext context, int doc) throws IOException {
		List<Explanation> subs = new ArrayList<>();
		boolean fail = false;
		int matchCount = 0;
		int shouldMatchCount = 0;
		Iterator<BooleanClause> cIter = query.iterator();
		for (Iterator<Weight> wIter = weights.iterator(); wIter.hasNext();) {
			Weight w = wIter.next();
			BooleanClause c = cIter.next();
			Explanation e = w.explain(context, doc);
			if (e.isMatch()) {
				if (c.isScoring()) {
					subs.add(e);
				} else if (c.isRequired()) {
					subs.add(Explanation.match(0f, "match on required clause, product of:",
							Explanation.match(0f, Occur.FILTER + " clause"), e));
				} else if (c.isProhibited()) {
					subs.add(Explanation.noMatch("match on prohibited clause (" + c.getQuery().toString() + ")", e));
					fail = true;
				}
				if (!c.isProhibited()) {
					matchCount++;
				}
				if (c.getOccur() == Occur.SHOULD) {
					shouldMatchCount++;
				}
			} else if (c.isRequired()) {
				subs.add(Explanation.noMatch("no match on required clause (" + c.getQuery().toString() + ")", e));
				fail = true;
			}
		}
		if (fail) {
			return Explanation.noMatch("Failure to meet condition(s) of required/prohibited clause(s)", subs);
		} else if (matchCount == 0) {
			return Explanation.noMatch("No matching clauses", subs);
		} else {
			// Replicating the same floating-point errors as the scorer does is quite
			// complex (essentially because of how ReqOptSumScorer casts intermediate
			// contributions to the score to floats), so in order to make sure that
			// explanations have the same value as the score, we pull a scorer and
			// use it to compute the score.
			Scorer scorer = scorer(context);
			int advanced = scorer.iterator().advance(doc);
			assert advanced == doc;
			return Explanation.match(scorer.score(), "sum of:", subs);
		}
	}

}
