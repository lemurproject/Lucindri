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
import java.util.List;

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;

public class IndriNearWeight extends IndriTermOpWeight {

	private final int distance;

	public IndriNearWeight(IndriNearQuery query, IndexSearcher searcher, String field, int distance, float boost)
			throws IOException {
		super(query, searcher, field, boost);
		this.distance = distance;
	}

	@Override
	protected IndriInvertedList createInvertedList(List<IndriDocAndPostingsIterator> iterators) throws IOException {
		IndriInvertedList invList = new IndriInvertedList(getField());

		IndriDocAndPostingsIterator iterator0 = iterators.get(0);
		iterator0.nextDoc();
		int currentDocID = iterator0.docID();
		boolean noMoreDocs = false;

		while (iterator0.docID() != DocIdSetIterator.NO_MORE_DOCS && !noMoreDocs) {

			// Find a document that has all the terms
			boolean hasDocMatch = true;
			for (int j = 1; j < iterators.size(); j++) {
				DocIdSetIterator iteratorj = iterators.get(j);
				int nextDocID = iteratorj.docID();
				while (nextDocID < currentDocID) {
					nextDocID = iteratorj.advance(currentDocID);
				}
				if (nextDocID == DocIdSetIterator.NO_MORE_DOCS) {
					noMoreDocs = true;
				}
				if (nextDocID != currentDocID) {
					hasDocMatch = false;
				}
			}

			// Find locations within document
			boolean locationMatch = true;
			if (hasDocMatch) {
				int[] numPostings = new int[iterators.size()];
				int[] nextStartPositions = new int[iterators.size()];
				int[] nextEndPositions = new int[iterators.size()];
				// Iterator over the first postings in the near
				for (int i = 0; i < iterator0.freq(); i++) {
					nextStartPositions[0] = iterator0.nextPosition();
					nextEndPositions[0] = iterator0.endPosition();
					numPostings[0] = i;
					// Iterate over the remaining terms/clauses in the near
					for (int j = 1; j < iterators.size(); j++) {
						IndriDocAndPostingsIterator iteratorj = iterators.get(j);
						// Increment the next posting until it is greater than the one before it
						while (nextStartPositions[j] < nextEndPositions[j - 1] && numPostings[j] < iteratorj.freq()) {
							nextStartPositions[j] = iteratorj.nextPosition();
							nextEndPositions[j] = iteratorj.endPosition();
							numPostings[j]++;
						}

						// Check the distance between term postings
						if (nextEndPositions[j - 1] >= nextStartPositions[j]
								|| nextEndPositions[j - 1] < (nextStartPositions[j] - distance)) {
							locationMatch = false;
						}
					}
					// Add the match to the inverted list
					if (locationMatch) {
						invList.addPosting(currentDocID, nextStartPositions[0], nextEndPositions[iterators.size() - 1]);

						for (int j = 1; j < iterators.size(); j++) {
							IndriDocAndPostingsIterator iteratorj = iterators.get(j);
							if (numPostings[j] < iteratorj.freq()) {
								nextStartPositions[j] = iteratorj.nextPosition();
								nextEndPositions[j] = iteratorj.endPosition();
								numPostings[j]++;
							} else {
								nextStartPositions[j] = -2 * distance;
								nextEndPositions[j] = -2 * distance;
							}
						}
					}
					locationMatch = true;
				}
			}

			currentDocID = iterator0.nextDoc();
		}
		return invList;

	}

}
