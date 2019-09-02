package org.lemurproject.lucindri.analyzer;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

/**
 * {@link Analyzer} for English.
 */
public final class EnglishAnalyzerConfigurable extends StopwordAnalyzerBase {

	public enum StemmerType {
		NONE, PORTER, KSTEM
	};

	private final CharArraySet stemExclusionSet;
	private Boolean doLowerCase = true;
	private Boolean doStopwordRemoval = true;
	private StemmerType stemmer = StemmerType.PORTER;

	/**
	 * Returns an unmodifiable instance of the default stop words set.
	 * 
	 * @return default stop words set.
	 */
	public static CharArraySet getDefaultStopSet() {
		return DefaultSetHolder.DEFAULT_STOP_SET;
	}

	/**
	 * Atomically loads the DEFAULT_STOP_SET in a lazy fashion once the outer class
	 * accesses the static final set the first time.;
	 */
	private static class DefaultSetHolder {
		static final CharArraySet DEFAULT_STOP_SET = EnglishAnalyzer.ENGLISH_STOP_WORDS_SET;
	}

	/**
	 * Builds an analyzer with the default stop words: {@link #getDefaultStopSet}.
	 */
	public EnglishAnalyzerConfigurable() {
		this(DefaultSetHolder.DEFAULT_STOP_SET);
	}

	/**
	 * Builds an analyzer with the given stop words.
	 * 
	 * @param matchVersion lucene compatibility version
	 * @param stopwords    a stopword set
	 */
	public EnglishAnalyzerConfigurable(CharArraySet stopwords) {
		this(stopwords, CharArraySet.EMPTY_SET);
	}

	/**
	 * Builds an analyzer with the given stop words. If a non-empty stem exclusion
	 * set is provided this analyzer will add a {@link SetKeywordMarkerFilter}
	 * before stemming.
	 * 
	 * @param matchVersion     lucene compatibility version
	 * @param stopwords        a stopword set
	 * @param stemExclusionSet a set of terms not to be stemmed
	 */
	public EnglishAnalyzerConfigurable(CharArraySet stopwords, CharArraySet stemExclusionSet) {
		super(stopwords);
		this.stemExclusionSet = CharArraySet.unmodifiableSet(CharArraySet.copy(stemExclusionSet));
	}

	/**
	 * Enable or disable the conversion of text to lower case.
	 */
	public void setLowercase(Boolean onOff) {
		this.doLowerCase = onOff;
	}

	/**
	 * Enable or disable stopword removal.
	 */
	public void setStopwordRemoval(Boolean onOff) {
		this.doStopwordRemoval = onOff;
	}

	/**
	 * Control whether and how stemming is done. See StemmerType.
	 */
	public void setStemmer(StemmerType s) {
		this.stemmer = s;
	}

	/**
	 * Creates a {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
	 * which tokenizes all the text in the provided {@link Reader}.
	 * 
	 * @return A {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
	 *         built from an {@link StandardTokenizer} filtered with
	 *         {@link StandardFilter}, {@link EnglishPossessiveFilter},
	 *         {@link LowerCaseFilter}, {@link StopFilter} ,
	 *         {@link SetKeywordMarkerFilter} if a stem exclusion set is provided
	 *         and {@link PorterStemFilter}.
	 */

	@Override
	protected TokenStreamComponents createComponents(String fieldName) {

		final Tokenizer source = new StandardTokenizer();
		// TokenStream result = new StandardFilter(source);
		// prior to this we get the classic behavior, standardfilter does it for us.

		TokenStream result = source;
		result = new EnglishPossessiveFilter(result);

		if (this.doLowerCase)
			result = new LowerCaseFilter(result);

		if (this.doStopwordRemoval)
			result = new StopFilter(result, stopwords);

		if (!stemExclusionSet.isEmpty())
			result = new SetKeywordMarkerFilter(result, stemExclusionSet);

		if (this.stemmer == StemmerType.PORTER)
			result = new PorterStemFilter(result);
		else if (this.stemmer == StemmerType.KSTEM)
			result = new KStemFilter(result);

		return new TokenStreamComponents(source, result);

	}
}
