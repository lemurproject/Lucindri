This is an implementation of Indri logic in the Lucene Search Engine.  There are two parts of this project: the indexer and the searcher.

Indexer:
The main class in indexer is: org.lemurproject.indexer.BuildIndex.  This program takes a single properties file as an argument.  See index.properties in the indexer directory as an example.

index.properties:
```
java -jar -Xmx16G LucindriIndexer.jar
```

Searcher:
The main class in searcher is: org.lemurproject.searcher.IndriSearch.  It takes an xml parameter file, which contains queries, as an argument.  See indriQueries.xml in the searcher directory as an example.  The query parameters follow the same format as Indri.  See https://lemurproject.org/doxygen/lemur/html/IndriRunQuery.html and https://sourceforge.net/p/lemur/wiki/Basic%20use%20of%20the%20Indri%20Query%20Language/ for reference.  Currently, this project has Dirichlet and Jelinek-Mercer smoothing rules (aka Similarity in Lucene) available.  This application prints the results in TREC format. 
