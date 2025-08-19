package org.jmouse.web.http;

import java.net.URLDecoder;

import static java.nio.charset.StandardCharsets.UTF_8;

final public class URINormalizer {

    private URINormalizer() {}

    public static String normalize(String uri, boolean doubleDecode) {
        String normalized = uri;

        if (normalized == null) {
            return "/";
        }

        try {
            normalized = normalized.replace('\\', '/');
            normalized = normalized.replaceAll("/+", "/");

            if (normalized.matches(".*\\p{Cntrl}+.*")) {
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

}
