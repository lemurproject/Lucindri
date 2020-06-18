package org.lemurproject.lucindri.indexer.documentwriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient.Builder;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.lemurproject.lucindri.indexer.domain.IndexingConfiguration;
import org.lemurproject.lucindri.indexer.domain.ParsedDocument;
import org.lemurproject.lucindri.indexer.domain.ParsedDocumentField;

public class SolrDocumentWriter implements DocumentWriter {

	private List<SolrInputDocument> docList;
	private CloudSolrClient solrClient;

	public SolrDocumentWriter(IndexingConfiguration options) {
		List<String> zkHosts = new ArrayList<String>();
		String zkHostString = String.join(":", options.getHost(), options.getPort());
		zkHosts.add(zkHostString);
		Builder builder = new CloudSolrClient.Builder(zkHosts, java.util.Optional.empty());
		// builder.withHttpClient(HttpClientUtil.createClient(null));
		solrClient = builder.build();
		solrClient.setDefaultCollection(options.getIndexName());
		docList = new ArrayList<SolrInputDocument>();
	}

	@Override
	public void writeDocuments(ParsedDocument parsedDoc) throws IOException {
		if (parsedDoc != null) {
			SolrInputDocument solrDocument = new SolrInputDocument();

			// Add document to search engine
			for (ParsedDocumentField docField : parsedDoc.getDocumentFields()) {
				if (docField.getContent() != null) {
					if (!docField.isNumeric()) {
						solrDocument.addField(docField.getFieldName(), docField.getContent());
					} else {
						solrDocument.addField(docField.getFieldName(), Long.valueOf(docField.getContent()).longValue());
					}
				}
			}
			// iWriter.addDocument(luceneDoc);
			docList.add(solrDocument);

		}
		if (docList.size() >= 500) {
			// Commit within 5 minutes.
			UpdateResponse resp;
			try {
				resp = solrClient.add(docList, 300000);
				solrClient.commit();
				if (resp.getStatus() != 0) {
					System.out.println("Some horrible error has occurred, status is: " + resp.getStatus());
				}
				docList.clear();
			} catch (SolrServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	@Override
	public void closeDocumentWriter() throws IOException {
		if (docList.size() > 0) {
			// Commit within 5 minutes.
			UpdateResponse resp;
			try {
				resp = solrClient.add(docList, 300000);
				solrClient.commit();
				if (resp.getStatus() != 0) {
					System.out.println("Some horrible error has occurred, status is: " + resp.getStatus());
				}
				docList.clear();
			} catch (SolrServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		solrClient.close();

	}

}
