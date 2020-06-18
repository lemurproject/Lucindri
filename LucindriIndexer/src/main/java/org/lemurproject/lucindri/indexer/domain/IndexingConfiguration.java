package org.lemurproject.lucindri.indexer.domain;

import java.io.Serializable;
import java.util.List;

public class IndexingConfiguration extends BaseObject implements Serializable {

	private static final long serialVersionUID = 1980324795467123134L;

	private String dataDirectory;
	private String indexDirectory;
	private String indexName;

	// Field Options
	private boolean indexFullText;
	private List<String> indexFields;

	// Analyzer Options
	private String stemmer;
	private boolean removeStopwords;
	private boolean ignoreCase;

	// Defines the type of parser
	private String documentFormat;

	// Solr Options
	private String host;
	private String port;

	public String getDataDirectory() {
		return dataDirectory;
	}

	public void setDataDirectory(String dataDirectory) {
		this.dataDirectory = dataDirectory;
	}

	public String getIndexDirectory() {
		return indexDirectory;
	}

	public void setIndexDirectory(String indexDirectory) {
		this.indexDirectory = indexDirectory;
	}

	public String getIndexName() {
		return indexName;
	}

	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}

	public String getDocumentFormat() {
		return documentFormat;
	}

	public void setDocumentFormat(String documentFormat) {
		this.documentFormat = documentFormat;
	}

	public String getStemmer() {
		return stemmer;
	}

	public void setStemmer(String stemmer) {
		this.stemmer = stemmer;
	}

	public boolean isRemoveStopwords() {
		return removeStopwords;
	}

	public void setRemoveStopwords(boolean removeStopwords) {
		this.removeStopwords = removeStopwords;
	}

	public boolean isIgnoreCase() {
		return ignoreCase;
	}

	public void setIgnoreCase(boolean ignoreCase) {
		this.ignoreCase = ignoreCase;
	}

	public boolean isIndexFullText() {
		return indexFullText;
	}

	public void setIndexFullText(boolean indexFullText) {
		this.indexFullText = indexFullText;
	}

	public List<String> getIndexFields() {
		return indexFields;
	}

	public void setIndexFields(List<String> indexFields) {
		this.indexFields = indexFields;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

}
