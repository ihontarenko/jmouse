package org.jmouse.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

public class StringHelper {

    public static String[] EMPTY_STRING_ARRAY = {};

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

}
