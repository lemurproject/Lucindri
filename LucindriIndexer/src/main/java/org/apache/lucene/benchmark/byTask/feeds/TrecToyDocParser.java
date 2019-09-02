package org.apache.lucene.benchmark.byTask.feeds;

import java.io.IOException;
import java.util.Properties;

public class TrecToyDocParser extends TrecDocParser {

  private static final String CLASS = "<CLASS>";
  private static final String CLASS_END = "</CLASS>";

  private static final String TEXT = "<TEXT>";
  private static final String TEXT_END = "</TEXT>";

  private static final String HEADLINE = "<HEADLINE>";
  private static final String HEADLINE_END = "</HEADLINE>";

  @Override
  public DocData parse(DocData docData, String name, TrecContentSource trecSrc,
      StringBuilder docBuf, ParsePathType pathType) throws IOException {

    String classString = getStringForTags(docBuf, CLASS, CLASS_END);
    String bodyString = getStringForTags(docBuf, TEXT, TEXT_END);
    String headlineString = getStringForTags(docBuf, HEADLINE, HEADLINE_END);

    docData.clear();
    docData.setName(name);
    docData.setBody(bodyString);

    Properties docProperties = new Properties();
    docProperties.setProperty("class", classString);
    docProperties.setProperty("headline", headlineString);
    docData.setProps(docProperties);
    return docData;
  }

  private String getStringForTags(StringBuilder docBuf, String beginTag, String endTag) {
    String returnString;
    int h1 = docBuf.indexOf(beginTag);
    if (h1 >= 0) {
      int h2 = docBuf.indexOf(endTag, h1);
      returnString = docBuf.substring(h1 + beginTag.length(), h2).trim();
    } else {
      returnString = new String();
    }
    return returnString;
  }

}
