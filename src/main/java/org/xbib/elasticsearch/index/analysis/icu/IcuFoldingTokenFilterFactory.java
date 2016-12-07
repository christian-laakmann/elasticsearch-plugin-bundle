package org.xbib.elasticsearch.index.analysis.icu;

import com.ibm.icu.text.FilteredNormalizer2;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UnicodeSet;
import org.apache.lucene.analysis.TokenStream;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;

import java.io.InputStream;

/**
 * Uses the {@link org.apache.lucene.analysis.icu.ICUFoldingFilter}.
 * Applies foldings from UTR#30 Character Foldings.
 * Can be filtered to handle certain characters in a specified way (see http://icu-project.org/apiref/icu4j/com/ibm/icu/text/UnicodeSet.html)
 * E.g national chars that should be retained, like unicodeSetFilter : "[^åäöÅÄÖ]".
 */
public class IcuFoldingTokenFilterFactory extends AbstractTokenFilterFactory {

    private final Normalizer2 normalizer;

    public IcuFoldingTokenFilterFactory(IndexSettings indexSettings, Environment environment, String name, Settings settings) {
        super(indexSettings, name, settings);
        String normalizationName = settings.get("name", "utr30");
        Normalizer2.Mode normalizationMode;
        switch (settings.get("mode", "compose")) {
            case "compose_contiguous":
                normalizationMode = Normalizer2.Mode.COMPOSE_CONTIGUOUS;
                break;
            case "decompose":
                normalizationMode = Normalizer2.Mode.DECOMPOSE;
                break;
            case "fcd":
                normalizationMode = Normalizer2.Mode.FCD;
                break;
            default:
                normalizationMode = Normalizer2.Mode.COMPOSE;
                break;
        }
        InputStream inputStream = null;
        if ("utr30".equals(normalizationName)) {
            inputStream = getClass().getResourceAsStream("/icu/utr30.nrm");
        }
        Normalizer2 base = Normalizer2.getInstance(inputStream, normalizationName, normalizationMode);
        String unicodeSetFilter = settings.get("unicodeSetFilter");
        if (unicodeSetFilter != null) {
            UnicodeSet unicodeSet = new UnicodeSet(unicodeSetFilter);
            unicodeSet.freeze();
            this.normalizer = new FilteredNormalizer2(base, unicodeSet);
        } else {
            this.normalizer = base;
        }
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new ICUNormalizer2Filter(tokenStream, normalizer);
    }
}
