package org.lemurproject.lucindri.indexer.domain;

import java.util.List;

public class JsonDocument extends BaseObject {

	private String docno;
	private String text;
	private List<JsonDocumentField> fields;

	public String getDocno() {
		return docno;
	}

	public void setDocno(String docno) {
		this.docno = docno;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public List<JsonDocumentField> getFields() {
		return fields;
	}

	public void setFields(List<JsonDocumentField> fields) {
		this.fields = fields;
	}

}
