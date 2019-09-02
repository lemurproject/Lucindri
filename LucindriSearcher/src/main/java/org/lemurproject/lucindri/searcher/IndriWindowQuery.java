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
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.IndriWindowWeight;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Weight;

public class IndriWindowQuery extends IndriProximityQuery {

	private final int distance;

	public IndriWindowQuery(List<BooleanClause> clauses, String field, int distance) {
		super(clauses, field);
		this.distance = distance;
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) throws IOException {
		return new IndriWindowWeight(this, searcher, getField(), distance, boost);
	}

}
