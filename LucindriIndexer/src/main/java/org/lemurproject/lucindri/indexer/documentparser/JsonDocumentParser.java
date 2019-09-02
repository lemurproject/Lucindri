package org.lemurproject.lucindri.indexer.documentparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.lemurproject.lucindri.indexer.domain.IndexingConfiguration;
import org.lemurproject.lucindri.indexer.domain.JsonDocument;
import org.lemurproject.lucindri.indexer.domain.ParsedDocument;
import org.lemurproject.lucindri.indexer.domain.ParsedDocumentField;
import org.lemurproject.lucindri.indexer.factory.ConfigurableAnalyzerFactory;
import org.xml.sax.SAXException;

import com.google.gson.Gson;

public class JsonDocumentParser extends DocumentParser {

	private int docNum;
	private Iterator<File> fileIterator;
	private BufferedReader br;
	private String nextLine;
	private Gson gson;
	private Analyzer analyzer;
	private List<String> fieldsToIndex;
	private boolean indexFullText;

	public JsonDocumentParser(IndexingConfiguration options) throws IOException {
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

				JsonDocument jsonDoc = gson.fromJson(nextLine, JsonDocument.class);

				ParsedDocument doc = new ParsedDocument();
				doc.setDocumentFields(new ArrayList<>());

				ParsedDocumentField internalIdField = new ParsedDocumentField(INTERNALID_FIELD, String.valueOf(docNum),
						false);
				doc.getDocumentFields().add(internalIdField);

				ParsedDocumentField externalIdField = new ParsedDocumentField(EXTERNALID_FIELD, jsonDoc.getDocno(),
						false);
				doc.getDocumentFields().add(externalIdField);

				// Index fullText (catch-all) field
				if (indexFullText) {
					ParsedDocumentField fullTextField = new ParsedDocumentField(FULLTEXT_FIELD, jsonDoc.getText(),
							false);
					doc.getDocumentFields().add(fullTextField);
				}

				// Index fields defined by user
				int tokenPosition = 0;
				TokenStream stream = analyzer.tokenStream(null, new StringReader(jsonDoc.getText()));
				CharTermAttribute cattr = stream.addAttribute(CharTermAttribute.class);
				stream.reset();

				for (int i = 0; i < jsonDoc.getFields().size(); i++) {
					if (fieldsToIndex.contains(jsonDoc.getFields().get(i).getName())) {
						int start = jsonDoc.getFields().get(i).getStart();
						int end = jsonDoc.getFields().get(i).getEnd();
						StringJoiner fieldBuffer = new StringJoiner(" ");
						while (stream.incrementToken() && tokenPosition < end) {
							tokenPosition++;
							if (tokenPosition >= start) {
								fieldBuffer.add(cattr.toString());
							}
						}
						ParsedDocumentField indriField = new ParsedDocumentField(jsonDoc.getFields().get(i).getName(),
								fieldBuffer.toString(), false);
						doc.getDocumentFields().add(indriField);
					}
				}
				stream.end();
				stream.close();

				return doc;
			}
		}
		return null;
	}

}
