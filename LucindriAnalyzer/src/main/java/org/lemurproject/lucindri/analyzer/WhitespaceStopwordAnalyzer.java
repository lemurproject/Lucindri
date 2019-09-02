package org.lemurproject.lucindri.analyzer;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;

public class WhitespaceStopwordAnalyzer extends StopwordAnalyzerBase {

	private final CharArraySet stemExclusionSet;

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
	public WhitespaceStopwordAnalyzer() {
		this(DefaultSetHolder.DEFAULT_STOP_SET);
	}

	/**
	 * Builds an analyzer with the given stop words.
	 * 
	 * @param matchVersion lucene compatibility version
	 * @param stopwords    a stopword set
	 */
	public WhitespaceStopwordAnalyzer(CharArraySet stopwords) {
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
	public WhitespaceStopwordAnalyzer(CharArraySet stopwords, CharArraySet stemExclusionSet) {
		super(stopwords);
		this.stemExclusionSet = CharArraySet.unmodifiableSet(CharArraySet.copy(stemExclusionSet));
	}

	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		final Tokenizer source = new WhitespaceTokenizer();
		TokenStream result = source;
		result = new StopFilter(result, stopwords);
		return new TokenStreamComponents(source, result);
	}

}
