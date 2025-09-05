package org.jmouse.web.http;

import java.net.URLDecoder;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

final public class HttpNormalizer {

    public static final Pattern P_CNTRL = Pattern.compile(".*\\p{Cntrl}+.*");

    private HttpNormalizer() {}

    public static class Options {

        private boolean doubleDecoded = false;

    }

    public static String normalize(String uri, boolean doubleDecode) {
        String normalized = uri;

        if (normalized == null) {
            return "/";
        }

        try {
            normalized = normalized.replace('\\', '/');
            normalized = normalized.replaceAll("/+", "/");

            if (P_CNTRL.matcher(normalized).matches()) {
                return null;
            }

            if (doubleDecode) {
                normalized = URLDecoder.decode(normalized, UTF_8);
                // if %XX present
                if (normalized.contains("%")) {
                    normalized = URLDecoder.decode(normalized, UTF_8);
                }
            }

            if (!normalized.startsWith("/")) {
                normalized = "/" + normalized;
            }

            normalized = normalized.replace("/./", "/");

            return normalized;
        } catch (Exception e) {
            return null;
        }
    }

    public static String normalizeQuery(String queryString) {
        String normalized = queryString;



        return normalized;
    }

}
