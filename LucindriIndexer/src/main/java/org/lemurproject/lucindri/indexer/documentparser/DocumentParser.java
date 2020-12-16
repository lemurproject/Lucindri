package org.lemurproject.lucindri.indexer.documentparser;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.lemurproject.lucindri.indexer.domain.ParsedDocument;
import org.xml.sax.SAXException;

public abstract class DocumentParser {

	public final static String FULLTEXT_FIELD = "fulltext";
	public final static String FULLTEXT_LENGTH_FIELD = "fulltextlength";
	public final static String EXTERNALID_FIELD = "id";
	public final static String INTERNALID_FIELD = "internalId";
	public final static String TITLE_FIELD = "title";
	public final static String URL_FIELD = "url";

	private final static Analyzer analyzer = new SimpleAnalyzer();

	/**
	 * 
	 * @return boolean defining whether another document exists
	 */
	public abstract boolean hasNextDocument();

	/**
	 * Examines input to find the next document and split it into fields.
	 * 
	 * @return
	 * @throws IOException
	 */
	public abstract ParsedDocument getNextDocument() throws IOException, SAXException;

	public static long countTokens(String text, String fieldName) throws IOException {
		if (text == null || text.length() == 0) {
			return 0l;
		} else {
			TokenStream ts = analyzer.tokenStream(fieldName, text);
			ts.reset();
			long count = 0;
			while (ts.incrementToken()) {
				count++;
			}
			ts.end();
			ts.close();
			return count;
		}
	}

}
