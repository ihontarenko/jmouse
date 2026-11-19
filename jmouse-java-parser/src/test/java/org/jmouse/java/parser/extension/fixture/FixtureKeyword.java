package org.jmouse.java.parser.extension.fixture;

import org.jmouse.java.parser.lexer.JavaKeywordCategory;
import org.jmouse.java.parser.lexer.JavaKeywordDefinition;

enum FixtureKeyword implements JavaKeywordDefinition {
    TRACE(FixtureNames.TRACE_KEYWORD),
    BUNDLE(FixtureNames.BUNDLE_KEYWORD);

    private final String spelling;

    FixtureKeyword(String spelling) {
        this.spelling = spelling;
    }

    @Override
    public String spelling() {
        return spelling;
    }

    @Override
    public JavaKeywordCategory category() {
        return JavaKeywordCategory.RESERVED;
    }
}
