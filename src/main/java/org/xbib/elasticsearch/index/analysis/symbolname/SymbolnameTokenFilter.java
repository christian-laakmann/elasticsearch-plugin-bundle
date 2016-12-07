package org.xbib.elasticsearch.index.analysis.symbolname;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PackedTokenAttributeImpl;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class SymbolnameTokenFilter extends TokenFilter {

    private static final Pattern pattern = Pattern.compile("\\P{L}", Pattern.UNICODE_CHARACTER_CLASS);

    private final LinkedList<PackedTokenAttributeImpl> tokens;

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);

    private State current;

    protected SymbolnameTokenFilter(TokenStream input) {
        super(input);
        this.tokens = new LinkedList<>();
    }

    @Override
    public final boolean incrementToken() throws IOException {
        if (!tokens.isEmpty()) {
            assert current != null;
            PackedTokenAttributeImpl token = tokens.removeFirst();
            restoreState(current);
            termAtt.setEmpty().append(token);
            posIncAtt.setPositionIncrement(0);
            return true;
        }
        if (input.incrementToken()) {
            process();
            if (!tokens.isEmpty()) {
                current = captureState();
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        tokens.clear();
        current = null;
    }

    protected void process() throws CharacterCodingException {
        String term = new String(termAtt.buffer(), 0, termAtt.length());
        for (CharSequence charSequence : process(term)) {
            if (charSequence != null) {
                PackedTokenAttributeImpl token = new PackedTokenAttributeImpl();
                token.append(charSequence);
                tokens.add(token);
            }
        }
    }

    protected Collection<CharSequence> process(String term) {
        Collection<CharSequence> variants = new LinkedList<>();
        StringBuffer sb = new StringBuffer();
        Matcher m = pattern.matcher(term);
        while (m.find()) {
            String symbol = m.group();
            Character ch = symbol.charAt(0);
            String symbolname = " __" + Character.getName(ch).toUpperCase()
                    .replaceAll("\\s", "").replaceAll("\\-", "") + "__";
            m.appendReplacement(sb, symbolname);
        }
        m.appendTail(sb);
        String variant = sb.toString().trim();
        if (!variant.equals(term)) {
            variants.add(variant);
            if (variant.indexOf(' ') > 0) {
                Collections.addAll(variants, variant.split("\\s"));
            }
        }
        return variants;
    }
}
