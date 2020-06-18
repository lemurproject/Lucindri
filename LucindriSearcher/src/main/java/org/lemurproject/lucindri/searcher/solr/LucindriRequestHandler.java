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
package org.lemurproject.lucindri.searcher.solr;

import java.util.StringJoiner;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.SolrIndexSearcher;
import org.lemurproject.lucindri.searcher.parser.IndriQueryParser;

public class LucindriRequestHandler extends RequestHandlerBase {

	private final static String EXTERNALID_FIELD = "externalId";

	public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
		String query = req.getParams().get("name");
		if (query != null) {
			query = query.replace("!", "#");
			rsp.add("greeting", "Lucindri query: " + query);
		} else {
			rsp.add("greeting", "Lucindri - No query defined");
		}

		SolrIndexSearcher searcher = req.getSearcher();

		// IndriIndexSearcher searcher = new
		// IndriIndexSearcher(req.getSearcher().getIndexReader());

		IndriQueryParser queryParser = new IndriQueryParser();
		Query test = queryParser.parseQuery(query);

		rsp.add("query", test.getClass().toString());

		StringJoiner trecResults = new StringJoiner("\n");
		if (test != null) {
			TopDocs hitDocs = searcher.search(test, 10);
			ScoreDoc[] scoreDocs = hitDocs.scoreDocs;

			int rank = 0;
			for (ScoreDoc scoreDoc : scoreDocs) {
				rank++;
				int docid = scoreDoc.doc;

				Document doc = searcher.doc(docid);
				String fileName = doc.get(EXTERNALID_FIELD);

				trecResults.add(String.join(" ", "1", "Q0", fileName, String.valueOf(rank),
						String.valueOf(scoreDoc.score), "lucene"));
			}
		}
		rsp.add("results", trecResults.toString());
	}

	public String getDescription() {
		return "This is a custom request handler that says hello.";
	}
}
