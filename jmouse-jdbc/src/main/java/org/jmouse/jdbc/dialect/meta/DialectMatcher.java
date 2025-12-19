package org.jmouse.jdbc.dialect.meta;

import org.jmouse.core.matcher.Matcher;

@FunctionalInterface
public interface DialectMatcher extends Matcher<DatabaseInfo> {

    static DialectMatcher byProductName(String token) {
        return info -> info.productName() != null
                && info.productName().toLowerCase().contains(token.toLowerCase());
    }

}
