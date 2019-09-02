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
import java.util.TreeMap;

import org.apache.lucene.util.BytesRef;

public class IndriTermOpEnum extends IndriDocAndPostingsIterator {

	private IndriInvertedList invList;
	private Integer currentDocID;
	private int positionIndex;
	private int endPostion;

	public IndriTermOpEnum(IndriInvertedList invList) {
		this.invList = invList;
//		if (invList.getDocPostings() != null && invList.getDocPostings().size() > 0) {
//			currentDocID = invList.getDocPostings().firstKey();
//			positionIndex = 0;
//		} else {
		currentDocID = -1;
		positionIndex = 0;
		endPostion = -1;
//		}
	}

	@Override
	public int docID() {
		return currentDocID.intValue();
	}

	@Override
	public int nextDoc() throws IOException {
		currentDocID = invList.getDocPostings().higherKey(currentDocID);
		if (currentDocID == null) {
			currentDocID = DocIdSetIterator.NO_MORE_DOCS;
		}
		return currentDocID;
	}

	@Override
	public int advance(int target) throws IOException {
		currentDocID = invList.getDocPostings().higherKey(target - 1);
		if (currentDocID == null) {
			currentDocID = DocIdSetIterator.NO_MORE_DOCS;
		}
		return currentDocID;
	}

	@Override
	public long cost() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int freq() throws IOException {
		TreeMap<Integer, IndriDocumentPosting> docPostings = invList.getDocPostings().get(currentDocID);
		int freq = 0;
		if (docPostings != null) {
			freq = docPostings.size();
		}
		return freq;
	}

	/**
	 * Increments to the next posting and returns the start position of the next
	 * posting
	 */
	@Override
	public int nextPosition() throws IOException {
		int nextPosition = -1;
		if (invList.getDocPostings() != null && invList.getDocPostings().size() > 0
				&& invList.getDocPostings().get(currentDocID) != null
				&& invList.getDocPostings().get(currentDocID).get(positionIndex) != null) {
			nextPosition = invList.getDocPostings().get(currentDocID).get(positionIndex).getStart();
			endPostion = invList.getDocPostings().get(currentDocID).get(positionIndex).getEnd();
			positionIndex++;
		}
		return nextPosition;
	}

	@Override
	public int endPosition() throws IOException {
		return endPostion;
	}

	@Override
	public int startOffset() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int endOffset() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public BytesRef getPayload() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public IndriInvertedList getInvList() {
		return invList;
	}

}
