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
package org.lemurproject.lucindri.searcher;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.lemurproject.lucindri.analyzer.EnglishAnalyzerConfigurable;
import org.lemurproject.lucindri.searcher.domain.JsonIndriQuery;
import org.lemurproject.lucindri.searcher.domain.JsonIndriQueryWrapper;
import org.lemurproject.lucindri.searcher.parser.IndriQueryParser;
import org.lemurproject.lucindri.searcher.similarities.IndriDirichletSimilarity;
import org.lemurproject.lucindri.searcher.similarities.IndriJelinekMercerSimilarity;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class IndriSearch {

	private final static String EXTERNALID_FIELD = "externalId";

	public static void main(String[] args)
			throws Exception {
		if (args.length != 1) {
			System.out.println("Specify parameter file");
			System.exit(0);
		}

		String parametersFilePath = args[0];
		String queryParameters = readFile(parametersFilePath);

		JsonIndriQueryWrapper queryWrapper = null;
		if (isXML(queryParameters)) {
			queryWrapper = parseXML(queryParameters);
		}

		if (queryWrapper != null) {
			String indexDir = queryWrapper.getIndex();

			IndexReader reader = null;
			IndexSearcher searcher = null;
			if (indexDir.contains(",")) {
				String[] dirs = indexDir.split(",");
				IndexReader[] subReaders = new IndexReader[dirs.length];
				int readerIndex = 0;
				for (String dirString : dirs) {
					Directory dir = FSDirectory.open(Paths.get(dirString.trim()));
					IndexReader subReader = DirectoryReader.open(dir);
					subReaders[readerIndex] = subReader;
					readerIndex++;
				}
				reader = new MultiReader(subReaders, true);
			} else {
				Directory dir = FSDirectory.open(Paths.get(indexDir));
				reader = DirectoryReader.open(dir);
			}
			searcher = new IndriIndexSearcher(reader);
			if (reader == null || searcher == null) {
				throw new Exception("Index Directory was not properly set");
			}

			Similarity similarity = new IndriDirichletSimilarity();
			if (queryWrapper.getRule() != null) {
				String similarityString = queryWrapper.getRule().toLowerCase();
				String[] similarityParams = similarityString.split(":");
				String similarityName = similarityParams[0];
				String parameter = null;
				if (similarityParams.length > 1) {
					parameter = similarityParams[1];
				}
				if (similarityName.equals("dirichlet") || similarityName.equals("dir") || similarityName.equals("d")) {
					if (parameter != null) {
						float mu = Float.valueOf(parameter).floatValue();
						similarity = new IndriDirichletSimilarity(mu);
					}
				} else if (similarityName.equals("jelinek-mercer") || similarityName.equals("jm")
						|| similarityName.equals("linear")) {
					similarity = new IndriJelinekMercerSimilarity();
					if (parameter != null) {
						float lambda = Float.valueOf(parameter).floatValue();
						similarity = new IndriJelinekMercerSimilarity(lambda);
					}
				}
			}
			searcher.setSimilarity(similarity);

			for (JsonIndriQuery query : queryWrapper.getQueries()) {
				IndriQueryParser queryParser = new IndriQueryParser();
				Query test = queryParser.parseQuery(query.getText());

				if (test != null) {
					TopDocs hitDocs = searcher.search(test, queryWrapper.getCount());
					ScoreDoc[] scoreDocs = hitDocs.scoreDocs;

					int rank = 0;
					for (ScoreDoc scoreDoc : scoreDocs) {
						rank++;
						int docid = scoreDoc.doc;

						Document doc = searcher.doc(docid);
						String fileName = doc.get(EXTERNALID_FIELD);

						System.out.println(String.join(" ", query.getNumber(), "Q0", fileName, String.valueOf(rank),
								String.valueOf(scoreDoc.score), "lucene"));
					}
				}
			}
		} else {
			System.out.println("Could not parse query parameters.  Please provide XML or json query parameters.");
		}

	}

	private static boolean isXML(String text) {
		if (!text.startsWith("<")) {
			return false;
		}
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		org.w3c.dom.Document doc = null;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(new InputSource(new StringReader(text)));
		} catch (ParserConfigurationException | SAXException | IOException e) {
			return false;
		}
		return true;
	}

	private static JsonIndriQueryWrapper parseXML(String text)
			throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		dBuilder = dbFactory.newDocumentBuilder();
		org.w3c.dom.Document doc = dBuilder.parse(new InputSource(new StringReader(text)));

		JsonIndriQueryWrapper queryWrapper = new JsonIndriQueryWrapper();
		if (doc.getElementsByTagName("index") != null) {
			queryWrapper.setIndex(doc.getElementsByTagName("index").item(0).getTextContent());
		} else {
			System.out.println("Index must be defined in query parameters.");
			System.exit(0);
		}

		if (doc.getElementsByTagName("count") != null) {
			queryWrapper.setCount(Integer.valueOf(doc.getElementsByTagName("count").item(0).getTextContent()));
		} else {
			queryWrapper.setCount(100);
		}

		if (doc.getElementsByTagName("rule") != null) {
			queryWrapper.setRule(doc.getElementsByTagName("rule").item(0).getTextContent());
		}

		List<JsonIndriQuery> queries = new ArrayList<>();
		for (int i = 0; i < doc.getElementsByTagName("query").getLength(); i++) {
			NodeList childNodes = doc.getElementsByTagName("query").item(i).getChildNodes();
			JsonIndriQuery query = new JsonIndriQuery();
			for (int j = 0; j < childNodes.getLength(); j++) {
				Node childNode = childNodes.item(j);
				if (childNode.getNodeName().equals("number")) {
					query.setNumber(childNode.getTextContent());
				} else if (childNode.getNodeName().equals("text")) {
					query.setText(childNode.getTextContent());
				}
			}
			queries.add(query);
		}
		queryWrapper.setQueries(queries);
		return queryWrapper;
	}

	private static String readFile(String filePath) {
		StringBuilder contentBuilder = new StringBuilder();
		try (Stream<String> stream = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)) {
			stream.forEach(s -> contentBuilder.append(s).append("\n"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return contentBuilder.toString();
	}

	public static Analyzer getConfigurableAnalyzer() {
		EnglishAnalyzerConfigurable an = new EnglishAnalyzerConfigurable();
		an.setLowercase(true);
		an.setStopwordRemoval(true);
		an.setStemmer(EnglishAnalyzerConfigurable.StemmerType.KSTEM);
		return an;
	}

}
