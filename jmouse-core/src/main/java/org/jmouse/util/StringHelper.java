package org.jmouse.util;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

public class StringHelper {

    public static final char[]   HEX_CHARACTERS     = "0123456789abcdef".toCharArray();
    public static final String[] EMPTY_STRING_ARRAY = {};

    public static String[] commaSeparated(String input) {
        return tokenize(input.trim(), ",");
    }

    public static List<String> keyValue(String input) {
        return List.of(tokenize(input, "="));
    }

    public static String[] tokenize(String string, char delimiter) {
        return tokenize(string, String.valueOf(delimiter));
    }

    public static String[] tokenize(String string, String delimiters) {
        StringTokenizer tokenizer  = new StringTokenizer(string, delimiters);
        List<String>    collection = new ArrayList<>();

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();

            token = token.trim();

            if (!token.isEmpty()) {
                collection.add(token);
            }
        }

        return toStringArray(collection);
    }

    public static String[] toStringArray(Collection<String> collection) {
        return collection.toArray(EMPTY_STRING_ARRAY);
    }

    public static byte[] digest(InputStream inputStream, String algorithm) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        byte[]        buffer = new byte[8192];

        int read;

        while ((read = inputStream.read(buffer)) > 0) {
            digest.update(buffer, 0, read);
        }

        return digest.digest();
    }

    public static String hex(byte[] bytes) {
        char[] output = new char[bytes.length * 2];
        int    i      = 0;

        for (byte b : bytes) {
            int v = b & 0xFF;
            output[i++] = HEX_CHARACTERS[v >>> 4];
            output[i++] = HEX_CHARACTERS[v & 0x0F];
        }

        return new String(output);
    }

    public static String toLength(String string, int length) {
        String newString = string;

        if (string.length() > length) {
            newString = string.substring(0, length);
        }

        return newString;
    }

    public static boolean isQuoted(String value) {
        return value.length() >= 2 && (
                value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"'
                        || value.charAt(0) == '\'' && value.charAt(value.length() - 1) == '\''
        );
    }

    public static String ensureQuoted(String value) {
        return isQuoted(value) ? value : quote(value);
    }

    public static String quote(String etag) {
        return "\"" + unquote(etag) + "\"";
    }

    public static String unquote(String value) {
        return !isQuoted(value) ? value : value.substring(1, value.length() - 1);
    }

}
