package org.jmouse.jdbc.intercept.guard;

import java.util.Locale;

/**
 * Lightweight SQL analyzer used for safety checks.
 *
 * <p>Performs normalization (removes literals and comments) and extracts
 * simple structural signals such as DDL, multi-statement, and WHERE presence.</p>
 */
public final class SQLScanner {

    /**
     * Scans SQL and produces a {@link SQLScan} with detected characteristics.
     *
     * <p>Analysis is heuristic and does not perform full SQL parsing.</p>
     *
     * @param sql raw SQL string
     * @return scan result with extracted flags
     */
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

    /**
     * Checks if SQL contains a statement separator ({@code ;}).
     *
     * @param sql normalized SQL
     * @return {@code true} if separator is present
     */
    private boolean containsStatementSeparator(String sql) {
        return sql.indexOf(';') >= 0;
    }

    /**
     * Checks if SQL starts with any of the given tokens.
     *
     * @param sql normalized SQL
     * @param tokens tokens to match
     * @return {@code true} if any token matches
     */
    private boolean startsWithAny(String sql, String... tokens) {
        String trimmed = sql.stripLeading();

        for (String token : tokens) {
            if (trimmed.startsWith(token + " ") || trimmed.equals(token)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if SQL contains a standalone word.
     *
     * <p>Matches only whole tokens (not substrings).</p>
     *
     * @param sql normalized SQL
     * @param word word to search
     * @return {@code true} if word is present
     */
    private boolean containsWord(String sql, String word) {
        int i = -1;

        while ((i = sql.indexOf(word, i + 1)) >= 0) {
            boolean lok = (i == 0) || !Character.isLetterOrDigit(sql.charAt(i - 1));
            int     end = (i + word.length());
            boolean rok = (end >= sql.length()) || !Character.isLetterOrDigit(sql.charAt(end));
            if (lok && rok) {
                return true;
            }
        }

        return false;
    }

    /**
     * Normalizes SQL by removing:
     * <ul>
     *     <li>string literals (single and double quoted)</li>
     *     <li>line comments ({@code -- ...})</li>
     *     <li>block comments ({@code /* ... *\/})</li>
     * </ul>
     *
     * <p>Preserves spacing to keep token boundaries stable.</p>
     *
     * @param sql raw SQL
     * @return normalized SQL
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
                    index++;
                    buffer.append(' ');
                }
                continue;
            }

            // begin comments
            if (!inSingle && !inDouble) {
                if (character == '-' && n == '-') {
                    inLineComment = true;
                    index++;
                    buffer.append(' ');
                    continue;
                }
                if (character == '/' && n == '*') {
                    inBlockComment = true;
                    index++;
                    buffer.append(' ');
                    continue;
                }
            }

            // single quotes
            if (!inDouble && character == '\'') {
                if (inSingle) {
                    if (n == '\'') {
                        index++;
                        buffer.append(' ');
                        continue;
                    }
                    inSingle = false;
                } else {
                    inSingle = true;
                }
                buffer.append(' ');
                continue;
            }

            // double quotes
            if (!inSingle && character == '"') {
                if (inDouble) {
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

            if (inSingle || inDouble) {
                buffer.append(' ');
                continue;
            }

            buffer.append(character);
        }

        return buffer.toString();
    }
}