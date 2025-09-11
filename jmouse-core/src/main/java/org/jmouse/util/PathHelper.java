package org.jmouse.util;

import org.jmouse.core.URIType;

import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

final public class PathHelper {

    public static final Pattern P_CNTRL = Pattern.compile(".*\\p{Cntrl}+.*");

    private PathHelper() {}

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

            // RFC-safe decode (never turns %2F into '/')
            normalized = URIType.PATH.normalize(normalized, UTF_8);
            if (doubleDecode) {
                normalized = URIType.PATH.normalize(normalized, UTF_8);
            }

            normalized = normalized.replace("/./", "/");

            return normalized;
        } catch (Exception e) {
            return null;
        }
    }

}
