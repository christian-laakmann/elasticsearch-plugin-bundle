package org.xbib.elasticsearch.index.analysis.hyphen;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeFactory;

import java.io.IOException;

/**
 *
 */
public final class HyphenTokenizer extends Tokenizer {

    public static final int ALPHANUM = 0;
    public static final int ALPHANUM_COMP = 1;
    public static final int NUM = 2;
    public static final int CJ = 3;
    /**
     * String token types that correspond to token type int constants
     */
    public static final String[] TOKEN_TYPES = new String[]{
            "<ALPHANUM>",
            "<ALPHANUM_COMP>",
            "<NUM>",
            "<CJ>"
    };
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
    private HyphenTokenizerImpl scanner;
    private int skippedPositions;
    private int maxTokenLength = StandardAnalyzer.DEFAULT_MAX_TOKEN_LENGTH;

    /**
     * Creates a new instance of the {@link HyphenTokenizer}.  Attaches
     * the <code>input</code> to the newly created JFlex scanner.
     */
    public HyphenTokenizer() {
        super();
        init();
    }

    /**
     * Creates a new {@link HyphenTokenizer} with a given {@link org.apache.lucene.util.AttributeFactory}
     *
     * @param factory factory
     */
    public HyphenTokenizer(AttributeFactory factory) {
        super(factory);
        init();
    }

    /**
     * @return max token length
     * @see #setMaxTokenLength
     */
    public int getMaxTokenLength() {
        return maxTokenLength;
    }

    /**
     * Set the max allowed token length.  Any token longer
     * than this is skipped.
     *
     * @param length length
     */
    public void setMaxTokenLength(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("maxTokenLength must be greater than zero");
        }
        this.maxTokenLength = length;
    }

    private void init() {
        this.scanner = new HyphenTokenizerImpl(input);
    }

    @Override
    public final boolean incrementToken() throws IOException {
        clearAttributes();
        skippedPositions = 0;
        while (true) {
            int tokenType = scanner.getNextToken();
            if (tokenType == HyphenTokenizerImpl.YYEOF) {
                return false;
            }
            if (scanner.yylength() <= maxTokenLength) {
                posIncrAtt.setPositionIncrement(skippedPositions + 1);
                scanner.getText(termAtt);
                final int start = scanner.yychar();
                offsetAtt.setOffset(correctOffset(start), correctOffset(start + termAtt.length()));
                typeAtt.setType(HyphenTokenizer.TOKEN_TYPES[tokenType]);
                return true;
            } else {
                skippedPositions++;
            }
        }
    }

    @Override
    public final void end() throws IOException {
        super.end();
        int finalOffset = correctOffset(scanner.yychar() + scanner.yylength());
        offsetAtt.setOffset(finalOffset, finalOffset);
        posIncrAtt.setPositionIncrement(posIncrAtt.getPositionIncrement() + skippedPositions);
    }

    @Override
    public void close() throws IOException {
        super.close();
        scanner.yyreset(input);
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        scanner.yyreset(input);
        skippedPositions = 0;
    }
}