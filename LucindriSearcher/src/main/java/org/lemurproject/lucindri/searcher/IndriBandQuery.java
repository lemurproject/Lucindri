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
import org.apache.lucene.search.IndriBandWeight;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Weight;

public class IndriBandQuery extends IndriProximityQuery {

	public IndriBandQuery(List<BooleanClause> clauses, String field) {
		super(clauses, field);
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) throws IOException {
		IndriBandQuery query = this;
		return new IndriBandWeight(this, searcher, getField(), boost);
	}

}
