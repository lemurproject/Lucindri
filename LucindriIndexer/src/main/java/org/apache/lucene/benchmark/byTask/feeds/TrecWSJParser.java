package org.apache.lucene.benchmark.byTask.feeds;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Properties;

public class TrecWSJParser extends TrecDocParser {

  private static final String DOCID = "<DOCID>";
  private static final String DOCID_END = "</DOCID>";

  private static final String HEADLINE = "<HL>";
  private static final String HEADLINE_END = "</HL>";

  // Used for dates in 87, 88, 89
  private static final String DATE = "<DD>";
  private static final String DATE_END = "</DD>";

  // WSJ changed tags starting in 1990 - used for dates in 90, 91, 92
  private static final String DATE2 = "<DATE>";
  private static final String DATE2_END = "</DATE>";

  private static final String SOURCE = "<SO>";
  private static final String SOURCE_END = "</SO>";

  private static final String CO = "<CO>";
  private static final String CO_END = "</CO>";

  private static final String SUBJECT = "<IN>";
  private static final String SUBJECT_END = "</IN>";

  private static final String GV = "<GV>";
  private static final String GV_END = "</GV>";

  private static final String SUMMARY = "<LP>";
  private static final String SUMMARY_END = "</LP>";

  private static final String DATELINE = "<DATELINE>";
  private static final String DATELINE_END = "</DATELINE>";

  private static final String TEXT = "<TEXT>";
  private static final String TEXT_END = "</TEXT>";

  @Override
  public DocData parse(DocData docData, String name, TrecContentSource trecSrc,
      StringBuilder docBuf, ParsePathType pathType) throws IOException {
    // optionally skip some of the text, set date (no title?)
    Date date = null;

    String docIdString = getStringForTags(docBuf, DOCID, DOCID_END);
    String titleString = getStringForTags(docBuf, HEADLINE, HEADLINE_END);

    date = getDateForTags(docBuf, DATE, DATE_END);
    if (date == null) {
      date = getDateForTags(docBuf, DATE2, DATE2_END);
    }

    String sourceString = getStringForTags(docBuf, SOURCE, SOURCE_END);
    String coString = getStringForTags(docBuf, CO, CO_END);
    String subjectString = getStringForTags(docBuf, SUBJECT, SUBJECT_END);
    String locationString = getStringForTags(docBuf, DATELINE, DATELINE_END);
    String govString = getStringForTags(docBuf, GV, GV_END);
    String summaryString = getStringForTags(docBuf, SUMMARY, SUMMARY_END);
    String bodyString = getStringForTags(docBuf, TEXT, TEXT_END);

    docData.clear();
    docData.setName(name);
    docData.setDate(date);
    docData.setBody(bodyString);
    docData.setTitle(titleString);

    Properties docProperties = new Properties();
    docProperties.setProperty("subj<br>ect", subjectString);
    docProperties.setProperty("dateline", locationString);
    docProperties.setProperty("internalId", docIdString);
    docProperties.setProperty("source", sourceString);
    docProperties.setProperty("co", coString);
    docProperties.setProperty("gv", govString);
    docProperties.setProperty("summary", summaryString);
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

  private Date getDateForTags(StringBuilder docBuf, String beginTag, String endTag) {
    Date date = null;
    String dateString;
    int h1 = docBuf.indexOf(beginTag);
    if (h1 >= 0) {
      int h2 = docBuf.indexOf(endTag, h1);
      dateString = docBuf.substring(h1 + beginTag.length(), h2).trim();
      dateString = String.join("", dateString.substring(0, 6), "19", dateString.substring(6));
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
      try {
        LocalDate localDate = LocalDate.parse(dateString, formatter);
        date = Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
      } catch (Exception e) {
        System.out.println("Could not parse date: " + dateString);
        date = new Date();
      }
    } else {
      dateString = new String();
    }
    return date;
  }

}
