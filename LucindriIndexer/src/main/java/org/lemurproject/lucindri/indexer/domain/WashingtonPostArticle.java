package org.lemurproject.lucindri.indexer.domain;

import java.util.List;

public class WashingtonPostArticle extends BaseObject {

	private String id;
	private String article_url;
	private String title;
	private List<WashingtonPostContent> contents;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<WashingtonPostContent> getContents() {
		return contents;
	}

	public void setContents(List<WashingtonPostContent> contents) {
		this.contents = contents;
	}

	public String getArticle_url() {
		return article_url;
	}

	public void setArticle_url(String article_url) {
		this.article_url = article_url;
	}
}
