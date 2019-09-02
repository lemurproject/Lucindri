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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.Terms;

public class IndriIndexSearcher extends IndexSearcher {

	private CollectionStatistics collectionStatistics;

	public IndriIndexSearcher(IndexReader r) {
		super(r);
	}

	@Override
	public CollectionStatistics collectionStatistics(String field) throws IOException {
		if (collectionStatistics == null) {
			long docCount = 0;
			long sumTotalTermFreq = 0;
			long sumDocFreq = 0;
			for (LeafReaderContext leaf : reader.leaves()) {
				final Terms terms = leaf.reader().terms(field);
				if (terms == null) {
					continue;
				}
				docCount += terms.getDocCount();
				// sumTotalTermFreq += terms.getSumTotalTermFreq();
				// System.out.println("Number of tokens (lucene): " +
				// terms.getSumTotalTermFreq());
				sumDocFreq += terms.getSumDocFreq();

				NumericDocValues numericDocValues = leaf.reader().getNormValues(field);
				int nextDoc = numericDocValues.nextDoc();
				int noNorm = 0;
				int normCount = 0;
				long tempFreq = 0;
				while (nextDoc != numericDocValues.NO_MORE_DOCS) {
					if (numericDocValues.longValue() < 1) {
						noNorm++;
					} else {
						normCount++;
					}
					long normValue = numericDocValues.longValue();
					tempFreq += normValue;
					sumTotalTermFreq += normValue;
					nextDoc = numericDocValues.nextDoc();
				}
				// System.out.println("Number of tokens calculated: " + tempFreq);
			}
			if (docCount == 0) {
				return null;
			}
			collectionStatistics = new CollectionStatistics(field, reader.maxDoc(), docCount, sumTotalTermFreq,
					sumDocFreq);
		}
		return collectionStatistics;
	}

}
