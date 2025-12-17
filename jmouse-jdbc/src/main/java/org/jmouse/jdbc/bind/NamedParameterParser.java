package org.jmouse.jdbc.bind;

import java.util.ArrayList;
import java.util.List;

public final class NamedParameterParser {

    public ParsedSQL parse(String sql) {
        StringBuilder buffer = new StringBuilder(sql.length());
        List<String> names = new ArrayList<>();

        boolean inSingleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);

            // End line comment
            if (inLineComment) {
                buffer.append(c);
                if (c == '\n') inLineComment = false;
                continue;
            }

            // End block comment
            if (inBlockComment) {
                buffer.append(c);
                if (c == '*' && i + 1 < sql.length() && sql.charAt(i + 1) == '/') {
                    buffer.append('/');
                    i++;
                    inBlockComment = false;
                }
                continue;
            }

            // Start comments (when not in string)
            if (!inSingleQuote) {
                if (c == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                    buffer.append(c).append('-');
                    i++;
                    inLineComment = true;
                    continue;
                }
                if (c == '/' && i + 1 < sql.length() && sql.charAt(i + 1) == '*') {
                    buffer.append(c).append('*');
                    i++;
                    inBlockComment = true;
                    continue;
                }
            }

            // Toggle string literal (single quotes)
            if (c == '\'') {
                buffer.append(c);
                // handle escaped '' inside string
                if (inSingleQuote && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    buffer.append('\'');
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
                    buffer.append("::");
                    i++;
                    continue;
                }

                int start = i + 1;
                if (start < sql.length() && isNameStart(sql.charAt(start))) {
                    int j = start + 1;
                    while (j < sql.length() && isNamePart(sql.charAt(j))) j++;

                    String name = sql.substring(start, j);
                    names.add(name);
                    buffer.append('?');
                    i = j - 1;
                    continue;
                }
            }

            buffer.append(c);
        }

        return new ParsedSQL(buffer.toString(), List.copyOf(names));
    }

    private static boolean isNameStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isNamePart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    public static void main(String[] args) {
        new NamedParameterParser().parse("select * from user where id = ? and name = :name");
    }

}
