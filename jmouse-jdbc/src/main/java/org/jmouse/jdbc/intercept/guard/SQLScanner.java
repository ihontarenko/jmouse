package org.jmouse.jdbc.intercept.guard;

import java.util.Locale;

public final class SQLScanner {

    public SQLScan scan(String sql) {
        if (sql == null || sql.isBlank()) {
            return new SQLScan(false, false, false, false);
        }

        String normalized = normalize(sql);
        String s          = normalized.trim().toLowerCase(Locale.ROOT);

        boolean hasSeparator = containsStatementSeparator(normalized);
        boolean ddl          = startsWithAny(s, "create", "alter", "drop", "truncate", "grant", "revoke");
        boolean isUD         = startsWithAny(s, "update", "delete");
        boolean hasWhere     = containsWord(s, "where");

        return new SQLScan(hasSeparator, ddl, isUD, hasWhere);
    }

    private boolean containsStatementSeparator(String sql) {
        return sql.indexOf(';') >= 0;
    }

    private boolean startsWithAny(String sql, String... tokens) {
        String trimmed = sql.stripLeading();

        for (String token : tokens) {
            if (trimmed.startsWith(token + " ") || trimmed.equals(token)) {
                return true;
            }
        }

        return false;
    }

    private boolean containsWord(String sql, String word) {
        int i = -1;

        while ((i = sql.indexOf(word, i + 1)) >= 0) {
            boolean leftOk = (i == 0) || !Character.isLetterOrDigit(sql.charAt(i - 1));
            int end = i + word.length();
            boolean rightOk = (end >= sql.length()) || !Character.isLetterOrDigit(sql.charAt(end));
            if (leftOk && rightOk) {
                return true;
            }
        }

        return false;
    }

    /**
     * Removes:
     * - single-quoted and double-quoted string literals
     * - line comments (-- ... \n)
     * - block comments (/* ... *\/)
     *
     * Keeps spacing so token boundaries remain mostly stable.
     */
    private String normalize(String sql) {
        StringBuilder buffer = new StringBuilder(sql.length());

        boolean inSingle       = false;
        boolean inDouble       = false;
        boolean inBlockComment = false;
        boolean inLineComment  = false;

        for (int index = 0; index < sql.length(); index++) {

            char character = sql.charAt(index);
            char n         = (index + 1 < sql.length()) ? sql.charAt(index + 1) : '\0';

            // end line comment
            if (inLineComment) {
                if (character == '\n' || character == '\r') {
                    inLineComment = false;
                    buffer.append(' ');
                }
                continue;
            }

            // end block comment
            if (inBlockComment) {
                if (character == '*' && n == '/') {
                    inBlockComment = false;
                    index++; // skip '/'
                    buffer.append(' ');
                }
                continue;
            }

            // begin comments (only if not inside strings)
            if (!inSingle && !inDouble) {
                if (character == '-' && n == '-') {
                    inLineComment = true;
                    index++; // skip second '-'
                    buffer.append(' ');
                    continue;
                }
                if (character == '/' && n == '*') {
                    inBlockComment = true;
                    index++; // skip '*'
                    buffer.append(' ');
                    continue;
                }
            }

            // handle single quotes
            if (!inDouble && character == '\'') {
                if (inSingle) {
                    // SQL escaping '' inside single-quoted strings
                    if (n == '\'') {
                        index++; // skip escaped quote
                        buffer.append(' '); // keep neutral
                        continue;
                    }
                    inSingle = false;
                } else {
                    inSingle = true;
                }
                buffer.append(' ');
                continue;
            }

            // handle double quotes
            if (!inSingle && character == '"') {
                if (inDouble) {
                    // escape "" inside identifier strings for some dialects
                    if (n == '"') {
                        index++;
                        buffer.append(' ');
                        continue;
                    }
                    inDouble = false;
                } else {
                    inDouble = true;
                }
                buffer.append(' ');
                continue;
            }

            // inside string: neutralize
            if (inSingle || inDouble) {
                buffer.append(' ');
                continue;
            }

            // default
            buffer.append(character);
        }

        return buffer.toString();
    }
}
