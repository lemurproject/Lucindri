/*
 * ===============================================================================================
 * Copyright (c) 2017 Carnegie Mellon University and University of Massachusetts. All Rights
 * Reserved.
 *
 * Use of the Lemur Toolkit for Language Modeling and Information Retrieval is subject to the terms
 * of the software license set forth in the LICENSE file included with this software, and also
 * available at http://www.lemurproject.org/license.html
 *
 * ================================================================================================
 */
package org.lemurproject.lucindri.indexer.documentwriter;

import java.io.IOException;

import org.lemurproject.lucindri.indexer.domain.ParsedDocument;

public interface DocumentWriter {

	void writeDocuments(ParsedDocument parsedDoc) throws IOException;

	void closeDocumentWriter() throws IOException;

}
