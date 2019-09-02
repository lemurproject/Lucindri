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

import java.util.TreeMap;

import org.apache.lucene.index.Term;

public class IndriInvertedList {

	private final String field;
	private int ctf;
	private int df;
	private TreeMap<Integer, TreeMap<Integer, IndriDocumentPosting>> docPostings;

	public IndriInvertedList(String field) {
		this.field = field;
		docPostings = new TreeMap<>();
	}

	public String getField() {
		return field;
	}

	public int getCtf() {
		return ctf;
	}

	public void setCtf(int ctf) {
		this.ctf = ctf;
	}

	public int getDf() {
		return df;
	}

	public void setDf(int df) {
		this.df = df;
	}

	public TreeMap<Integer, TreeMap<Integer, IndriDocumentPosting>> getDocPostings() {
		return docPostings;
	}

	public void setDocPostings(TreeMap<Integer, TreeMap<Integer, IndriDocumentPosting>> docPostings) {
		this.docPostings = docPostings;
	}

	public void addPosting(Integer docID, Integer startLocation, Integer endLocation) {
		docPostings.putIfAbsent(docID, new TreeMap<>());
		// Check if a posting that includes this start and end position already exist
		TreeMap<Integer, IndriDocumentPosting> postings = docPostings.get(docID);
		boolean addPosting = true;
		for (IndriDocumentPosting posting : postings.values()) {
			if (startLocation >= posting.getStart() && endLocation <= posting.getEnd()) {
				addPosting = false;
			}
		}
		if (addPosting) {
			IndriDocumentPosting posting = new IndriDocumentPosting(startLocation, endLocation);
			docPostings.get(docID).put(startLocation, posting);
		}
	}

	public TermStatistics getTermStatistics() {
		Term dummyTerm = new Term(field, "NEAR");
		int docFreq = docPostings.size();
		int totalTermFreq = 0;
		for (TreeMap<Integer, IndriDocumentPosting> postings : docPostings.values()) {
			totalTermFreq += postings.size();
		}
		TermStatistics termStats = new TermStatistics(dummyTerm.bytes(), docFreq, totalTermFreq);

		return termStats;
	}

}
