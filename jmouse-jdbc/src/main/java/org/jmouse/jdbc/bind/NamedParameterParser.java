package org.jmouse.jdbc.bind;

import java.util.ArrayList;
import java.util.List;

public final class NamedParameterParser {

    public ParsedSQL parse(String sql) {
        StringBuilder out = new StringBuilder(sql.length());
        List<String> names = new ArrayList<>();

        boolean inSingleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);

            // End line comment
            if (inLineComment) {
                out.append(c);
                if (c == '\n') inLineComment = false;
                continue;
            }

            // End block comment
            if (inBlockComment) {
                out.append(c);
                if (c == '*' && i + 1 < sql.length() && sql.charAt(i + 1) == '/') {
                    out.append('/');
                    i++;
                    inBlockComment = false;
                }
                continue;
            }

            // Start comments (when not in string)
            if (!inSingleQuote) {
                if (c == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                    out.append(c).append('-');
                    i++;
                    inLineComment = true;
                    continue;
                }
                if (c == '/' && i + 1 < sql.length() && sql.charAt(i + 1) == '*') {
                    out.append(c).append('*');
                    i++;
                    inBlockComment = true;
                    continue;
                }
            }

            // Toggle string literal (single quotes)
            if (c == '\'') {
                out.append(c);
                // handle escaped '' inside string
                if (inSingleQuote && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    out.append('\'');
                    i++;
                    continue;
                }
                inSingleQuote = !inSingleQuote;
                continue;
            }

            // Named parameter (when not in string)
            if (!inSingleQuote && c == ':') {
                // Skip postgres cast ::
                if (i + 1 < sql.length() && sql.charAt(i + 1) == ':') {
                    out.append("::");
                    i++;
                    continue;
                }

                int start = i + 1;
                if (start < sql.length() && isNameStart(sql.charAt(start))) {
                    int j = start + 1;
                    while (j < sql.length() && isNamePart(sql.charAt(j))) j++;

                    String name = sql.substring(start, j);
                    names.add(name);
                    out.append('?');
                    i = j - 1;
                    continue;
                }
            }

            out.append(c);
        }

        return new ParsedSQL(out.toString(), List.copyOf(names));
    }

    private static boolean isNameStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isNamePart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
