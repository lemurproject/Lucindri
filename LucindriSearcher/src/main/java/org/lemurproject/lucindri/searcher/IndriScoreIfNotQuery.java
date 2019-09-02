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
package org.lemurproject.lucindri.searcher;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.IndriScoreIfNotWeight;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Weight;

public class IndriScoreIfNotQuery extends IndriQuery {
	private List<BooleanClause> clauses; // used for toString() and getClauses()

	public IndriScoreIfNotQuery(List<BooleanClause> clauses) {
		super(clauses);
		this.clauses = clauses;
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) throws IOException {
		IndriQuery query = this;
		IndriQuery exclude = null;
		for (BooleanClause c : clauses) {
			if (c.getOccur() == Occur.MUST_NOT) {
				exclude = (IndriQuery) c.getQuery();
			} else {
				query = (IndriQuery) c.getQuery();
			}
		}
		Weight weight = new IndriScoreIfNotWeight(exclude, query, searcher, scoreMode, boost);
		return weight;
	}

}
