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
import java.util.List;

import org.lemurproject.lucindri.searcher.IndriSynonymQuery;

public class IndriSynonymWeight extends IndriTermOpWeight {

	public IndriSynonymWeight(IndriSynonymQuery query, IndexSearcher searcher, String field, float boost)
			throws IOException {
		super(query, searcher, field, boost);
	}

	protected IndriInvertedList createInvertedList(List<IndriDocAndPostingsIterator> iterators) throws IOException {
		IndriInvertedList invList = new IndriInvertedList(getField());

		for (IndriDocAndPostingsIterator iterator : iterators) {
			int docId = iterator.nextDoc();
			while (iterator.docID() != DocIdSetIterator.NO_MORE_DOCS) {
				for (int i = 0; i < iterator.freq(); i++) {
					int startPostion = iterator.nextPosition();
					int endPostion = iterator.endPosition();
					invList.addPosting(docId, startPostion, endPostion);
				}
				docId = iterator.nextDoc();
			}
		}
		return invList;

	}

}
