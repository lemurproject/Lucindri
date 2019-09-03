Lucindri
========

Lucindri is an open-source implementation of Indri search logic and structured query language using the Lucene Search Engine.  Lucindri consists of two components: the indexer and the searcher.

## Getting Started

## Lucindri Indexer
The main class in indexer is: org.lemurproject.indexer.BuildIndex.  This program takes a single properties file as an argument.  See index.properties in the indexer directory as an example.

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
#If index.fulltext is set to true, a field with all document text is created
indexFullText=[true | false]
fieldNames=[Comma separated list of field names to be stored (e.g. title, url, body)]

#analyzer options
stemmer=[kstem | porter | none]
removeStopwords=[true | false]
ignoreCase=[true | false]
```

To run Lucinidri indexer:
```
java -jar -Xmx16G LucindriIndexer index.properties
```

## Lucindri Searcher
The main class in searcher is: org.lemurproject.searcher.IndriSearch.  It takes an xml parameter file, which contains queries, as an argument.  See indriQueries.xml in the searcher directory as an example.  The query parameters follow the same format as Indri.  See https://lemurproject.org/doxygen/lemur/html/IndriRunQuery.html and https://sourceforge.net/p/lemur/wiki/Basic%20use%20of%20the%20Indri%20Query%20Language/ for reference.  Currently, this project has Dirichlet and Jelinek-Mercer smoothing rules (aka Similarity in Lucene) available.  This application prints the results in TREC format. 
