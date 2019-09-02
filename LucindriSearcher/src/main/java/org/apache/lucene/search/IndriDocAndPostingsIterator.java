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

import org.apache.lucene.index.PostingsEnum;

public abstract class IndriDocAndPostingsIterator extends PostingsEnum {

	@Override
	public abstract int docID();

	@Override
	public abstract int nextDoc() throws IOException;

	@Override
	public abstract int advance(int target) throws IOException;

	@Override
	public abstract long cost();

	public abstract int endPosition() throws IOException;

}
