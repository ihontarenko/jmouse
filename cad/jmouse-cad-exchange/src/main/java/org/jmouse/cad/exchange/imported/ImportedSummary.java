package org.jmouse.cad.exchange.imported;

import java.util.regex.Pattern;

/** What a converted document says of itself, as another product said it, cut to the length a summary may be. */
public final class ImportedSummary {

    /** The summary a catalogue object may carry, in characters. */
    private static final int LENGTH = 480;

    private static final Pattern TAG = Pattern.compile("<[^>]*>");
    private static final Pattern SPACE = Pattern.compile("\\s+");

    private ImportedSummary() {
    }

    /** A text cut at the last word that fits, an ellipsis after it; a short one as it is. */
    public static String shortened(String text) {
        if (text.length() <= LENGTH) {
            return text;
        }
        int space = text.lastIndexOf(' ', LENGTH);
        return text.substring(0, space > 0 ? space : LENGTH) + "…";
    }

    /** A description written as HTML — Eagle's —, its tags and entities read as the text they show, then shortened. */
    public static String ofHtml(String html) {
        String text = TAG.matcher(html).replaceAll(" ")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace("&nbsp;", " ")
                .replace("&amp;", "&");
        return shortened(SPACE.matcher(text).replaceAll(" ").strip());
    }
}
