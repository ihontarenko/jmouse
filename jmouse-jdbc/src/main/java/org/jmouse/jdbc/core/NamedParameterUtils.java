package org.jmouse.jdbc.core;

import java.util.*;

/**
 * 🧩 Parses SQL with :named parameters → JDBC '?' + ordered names.
 * Supports:
 *   - :name
 *   - Escaping with '::' produces a single ':' literal
 *   - Skips string literals ('...' or "...")
 */
final class NamedParameterUtils {

    static NamedSql parse(String sql) {
        List<String> names = new ArrayList<>();
        StringBuilder out = new StringBuilder(sql.length() + 16);

        boolean inSingle = false, inDouble = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);

            if (c == '\'' && !inDouble) { inSingle = !inSingle; out.append(c); continue; }
            if (c == '"'  && !inSingle) { inDouble = !inDouble; out.append(c); continue; }

            if (!inSingle && !inDouble && c == ':') {
                // Escaped '::' → ':'
                if (i + 1 < sql.length() && sql.charAt(i + 1) == ':') {
                    out.append(':'); i++; continue;
                }
                // parse name
                int j = i + 1;
                while (j < sql.length()) {
                    char ch = sql.charAt(j);
                    if (Character.isLetterOrDigit(ch) || ch == '_' ) j++; else break;
                }
                if (j == i + 1) { // just ':' with no name
                    out.append(':'); continue;
                }
                String name = sql.substring(i + 1, j);
                names.add(name);
                out.append('?');
                i = j - 1;
            } else {
                out.append(c);
            }
        }
        return new NamedSql(out.toString(), names);
    }

    static Object[] buildArgs(NamedSql ns, SqlParameterSource params) {
        Object[] args = new Object[ns.names().size()];
        for (int i = 0; i < args.length; i++) {
            String n = ns.names().get(i);
            if (!params.has(n)) throw new IllegalArgumentException("Missing named param: " + n);
            args[i] = params.get(n);
        }
        return args;
    }
}
