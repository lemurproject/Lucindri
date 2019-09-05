Lucindri
========

Lucindri is an open-source implementation of Indri search logic and structured query language using the Lucene Search Engine.  Lucindri consists of two components: the indexer and the searcher.

## Getting Started
Lucindri requires the 64-bit version of Java 11.  If you don't have it already, download the [Java 11 JDK](https://www.oracle.com/technetwork/java/javase/downloads/jdk11-downloads-5066655.html). Apache [Maven](https://maven.apache.org/download.cgi) is also required.

To get started, first clone [trec-car-tools](https://github.com/TREMA-UNH/trec-car-tools-java) from the Trema Lab at UNH.  

After cloning the trec-car-tools, build using Maven:
```
mvn clean install
```
Next, clone this repository and build using *mvn clean install* in this order:
+ LucindriAnalyzer
+ LucindriSearcher
+ LucindriIndexer


## Lucindri Indexer
The main class in indexer is: org.lemurproject.lucindri.indexer.BuildIndex.  This program takes a single properties file as an argument.  See index.properties in the indexer directory as an example.

Description of indexing properties:
```
#implementation options
# documentFormat options = text, wsj, gov2, json, wapo, warc, trectext, cw09, cw12, car, marco
documentFormat=[text | wsj | gov2 | json | wapo | warc | trectext | cw09 | cw12 | car | marco]

#data options
dataDirectory=[Directory or file where data is] 
indexDirectory=[Directory where index will be written]
indexName=[Name of the index]

#field options
#If index.fulltext is set to true, a field with all document text is created.  This is recommended.
#fulltext is the default field for queries if it is indexed
indexFullText=[true (recommended) | false]
fieldNames=[Comma separated list of field names to be stored (e.g. title, url, body)]

#analyzer options
stemmer=[kstem | porter | none]
removeStopwords=[true | false]
ignoreCase=[true | false]
```

Example index.properties:
```
#implementation options
# documentFormat options = text, wsj, gov2, json, wapo, warc, trectext, cw09, cw12, car, marco
documentFormat=cw09

#data options
dataDirectory=/usr/home/data/cw09data
indexDirectory=/usr/home/
indexName=CW09_lucindri_index

#field options
#If index.fulltext is set to true, a field with all document text is created.  This is recommended.
#fulltext is the default field for queries if it is indexed
indexFullText=true
fieldNames=title,url

#analyzer options
stemmer=kstem
removeStopwords=true
ignoreCase=true
```

Running the LucindriIndexer can be done from inside an IDE, invoking the main class (org.lemurproject.lucindri.indexer.BuildIndex), or using the jar file in the *target* directory.  Use at least 2G of heap space (preferably 4G - 8G).
```
java -jar -Xmx4G LucindriIndexer.jar index.properties
```

## Lucindri Searcher
The Lucindri Searcher has Indri Dirichlet and Jelinek-Mercer smoothing rules (a.k.a. Similarity in Lucene) implemented.  The results are printed in TREC format.

The main class in searcher is: org.lemurproject.lucindri.searche.IndriSearch.  It takes an xml parameter file, which contains queries, as an argument.  The query parameters follow the same format as Indri.  

### Retrieval Parameters
+ **index:** path to an Indri Repository. Specified as <index>/path/to/repository</index> in the parameter file and as -index=/path/to/repository on the command line. This element can be specified multiple times to combine Repositories.
+ **count:** an integer value specifying the maximum number of results to return for a given query. Specified as <count>number</count> in the parameter file and as -count=number on the command line.
+ **query:** An indri query language query to run. This element can be specified multiple times.
+ **rule:** specifies the smoothing rule (TermScoreFunction) to apply.
  + Format of the rule is: ( key ":" value ) [ "," key ":" value ]*

**Valid methods:**
+ dirichlet
(also 'd', 'dir') (default mu=2000)
+ jelinek-mercer
(also 'jm', 'linear') (default collectionLambda=0.4), collectionLambda is also known as just "lambda", either will work

Here is an example rule  in parameter file format:
```
<rule>dirichlet:2000</rule>
```
OR
```
<rule>jm:.3</rule>
```

This corresponds to Dirichlet smoothing with mu equal to 2000.

Here is an example query file:
```
<parameters>
        <index>PATH_TO_INDEX</index>
        <trecFormat>true</trecFormat>
        <rule>dirichlet:2000</rule>
        <count>100</count>
  <query>
    <number> 51 </number>
    <text>#5(president clinton)</text>
  </query>
  <query>
     <number> 52 </number>
     <text> #combine( avp ) </text>
   </query>
</parameters>
```

Running the LucindriSearcher can be done from inside an IDE, invoking the main class (org.lemurproject.lucindri.searcher.IndriSearch), or using the jar file in the *target* directory.  Use at least 2G of heap space (preferably 4G - 8G).
```
java -jar -Xmx4G LucindriSearcher.jar queries.xml
```

## Lucindri Query Language

### Lucindri Fields
Lucindri documents are stored in fields, which are specified at index time.  If indexFullText is set to true during indexing, a *fulltext* field is created and is used as the default query field if no field is specified.

You can search any field by typing the term you are looking for followed by a period "." and then the field name.

For example:
```
President.fulltext Obama.title
```

### Lucindri implements these Indri belief operators:
+ #combine (equivalent to #and)
  + Example: #combine(dog training)
+ #or
  + Example: #or(dog cat)
+ #not
  + Example: #and(president #not(obama))
+ #wand (weighted and)
  + Example: #wand(0.2 president 0.8 obama)
+ #wsum (weighted sum)
  + Example: #wsum(0.2 presdient 0.8 obama)
+ #max
  + Example: #max(dog train) - returns maximum of b(dog) and b(train)
+ #scoreif (filter require)
  + Example: #scoreif( sheep #combine(dolly cloning) ) - only consider those documents matching the query "sheep" and rank them according to the query #combine(dolly cloning)
+ #scoreifnot (filter reject)
  + Example: #scoreifnot( parton #combine(dolly cloning) ) - only consider those documents NOT matching the query "parton" and rank them according to the query #combine(dolly cloning)

And these term operators:
+ #band (boolean and)
  + #band(Q) is scored as #uw(Q) - an unordered window of the length of the document
+ #N (also known as #nearN and #windowN)
  + ordered window - terms must appear ordered, with at most N-1 terms between each
  + Example: #2(white house) - matches "white * house" (where * is any word or null)
+ #uwN (unordered window)
  + unordered window - all terms must appear within window of length N in any order
  + Example: #uw2(white house) - matches "white house" and "house white"
+ #syn (synonym)
  + Example: #syn( #1(united states) #1(united states of america) )
