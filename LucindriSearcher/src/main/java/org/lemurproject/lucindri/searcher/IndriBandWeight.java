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
import java.util.TreeMap;

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;

public class IndriBandWeight extends IndriTermOpWeight {

	public IndriBandWeight(IndriBandQuery query, IndexSearcher searcher, String field, float boost) throws IOException {
		super(query, searcher, field, boost);
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
			if (hasDocMatch) {
				// Sort all terms in order of start position
				TreeMap<Integer, TermInformation> sortedTerms = new TreeMap<>();
				int termNumber = 0;
				for (IndriDocAndPostingsIterator iterator : iterators) {
					int numPositions = 0;
					int previousPosition = -1;
					while (numPositions < iterator.freq()) {
						TermInformation termInfo = new TermInformation();
						termInfo.termNumber = termNumber;
						termInfo.start = iterator.nextPosition();
						termInfo.end = iterator.endPosition();
						termInfo.previousTermPosition = previousPosition;

						sortedTerms.put(Integer.valueOf(termInfo.start), termInfo);

						numPositions++;
						previousPosition = termInfo.start;
					}
					termNumber++;
				}

				// Loop over all terms
				List<Integer> sortedTermPositions = new ArrayList<Integer>(sortedTerms.keySet());
				for (int i = 0; i < sortedTermPositions.size(); i++) {
					int termsFound = 1;
					int current;

					for (current = i + 1; current < sortedTermPositions.size()
							&& termsFound != iterators.size(); current++) {
						// if the last time this term appeared was before the beginning of this window,
						// then this is a new term for this window
						if (sortedTerms.get(sortedTermPositions.get(current)).previousTermPosition < sortedTermPositions
								.get(i)) {
							termsFound++;
						}
					}
					if (termsFound == iterators.size()) {
						invList.addPosting(currentDocID, sortedTerms.get(sortedTermPositions.get(i)).start,
								sortedTerms.get(sortedTermPositions.get(current - 1)).end);
					}
					i = current - 1;
				}
			}
			currentDocID = iterator0.nextDoc();
		}

		return invList;

	}

	private class TermInformation {
		public int termNumber;
		public int start;
		public int end;
		public int previousTermPosition;
		public int weight;
	}

}
