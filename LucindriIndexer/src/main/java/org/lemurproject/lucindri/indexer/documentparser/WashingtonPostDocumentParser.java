package org.lemurproject.lucindri.indexer.documentparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

import org.apache.lucene.analysis.Analyzer;
import org.lemurproject.lucindri.indexer.domain.IndexingConfiguration;
import org.lemurproject.lucindri.indexer.domain.ParsedDocument;
import org.lemurproject.lucindri.indexer.domain.ParsedDocumentField;
import org.lemurproject.lucindri.indexer.domain.WashingtonPostArticle;
import org.lemurproject.lucindri.indexer.domain.WashingtonPostContent;
import org.lemurproject.lucindri.indexer.factory.ConfigurableAnalyzerFactory;
import org.xml.sax.SAXException;

import com.google.gson.Gson;

public class WashingtonPostDocumentParser extends DocumentParser {

	private int docNum;
	private Iterator<File> fileIterator;
	private BufferedReader br;
	private String nextLine;
	private Gson gson;
	private Analyzer analyzer;
	private List<String> fieldsToIndex;
	private boolean indexFullText;

	public WashingtonPostDocumentParser(IndexingConfiguration options) throws IOException {
		gson = new Gson();
		File folder = Paths.get(options.getDataDirectory()).toFile();
		fileIterator = Arrays.asList(folder.listFiles()).iterator();
		getNextScanner();
		nextLine = "";
		ConfigurableAnalyzerFactory analyzerFactory = new ConfigurableAnalyzerFactory();
		analyzer = analyzerFactory.getConfigurableAnalyzer(options);
		docNum = 0;
		fieldsToIndex = options.getIndexFields();
		indexFullText = options.isIndexFullText();
	}

	private void getNextScanner() throws IOException {
		if (fileIterator.hasNext()) {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(fileIterator.next())));
		} else {
			br = null;
		}
	}

	@Override
	public boolean hasNextDocument() {
		return fileIterator.hasNext() || nextLine != null;
	}

	@Override
	public ParsedDocument getNextDocument() throws IOException, SAXException {
		if (br != null) {
			if ((nextLine = br.readLine()) == null) {
				br.close();
				getNextScanner();
				if (br != null) {
					nextLine = br.readLine();
				}
			}
			if (nextLine != null) {

				docNum++;

				try {
					WashingtonPostArticle jsonDoc = gson.fromJson(nextLine, WashingtonPostArticle.class);

					ParsedDocument doc = new ParsedDocument();
					doc.setDocumentFields(new ArrayList<>());
					StringJoiner fullTextBuffer = new StringJoiner(" ");

					ParsedDocumentField internalIdField = new ParsedDocumentField(INTERNALID_FIELD,
							String.valueOf(docNum), false);
					doc.getDocumentFields().add(internalIdField);

					ParsedDocumentField externalIdField = new ParsedDocumentField(EXTERNALID_FIELD, jsonDoc.getId(),
							false);
					fullTextBuffer.add(jsonDoc.getId());
					doc.getDocumentFields().add(externalIdField);

					ParsedDocumentField titleField = new ParsedDocumentField(TITLE_FIELD, jsonDoc.getTitle(), false);
					fullTextBuffer.add(jsonDoc.getTitle());
					if (fieldsToIndex.contains(TITLE_FIELD)) {
						doc.getDocumentFields().add(titleField);
					}

					ParsedDocumentField urlField = new ParsedDocumentField(URL_FIELD, jsonDoc.getArticle_url(), false);
					fullTextBuffer.add(jsonDoc.getArticle_url());
					if (fieldsToIndex.contains(URL_FIELD)) {
						doc.getDocumentFields().add(urlField);
					}

					StringJoiner contentBuffer = new StringJoiner(" ");
					for (WashingtonPostContent wapoContent : jsonDoc.getContents()) {
						if (wapoContent.getContent() != null) {
							contentBuffer.add(wapoContent.getContent());
						}
					}
					ParsedDocumentField contentField = new ParsedDocumentField("content", contentBuffer.toString(),
							false);
					fullTextBuffer.add(contentBuffer.toString());
					if (fieldsToIndex.contains("content")) {
						doc.getDocumentFields().add(contentField);
					}

					// Index fullText (catch-all) field
					if (indexFullText) {
						ParsedDocumentField fullTextField = new ParsedDocumentField(FULLTEXT_FIELD,
								fullTextBuffer.toString(), false);
						doc.getDocumentFields().add(fullTextField);
					}

					return doc;
				} catch (Exception e) {

				}
			}
		}
		return null;
	}

}
