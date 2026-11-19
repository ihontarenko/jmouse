package org.jmouse.java.parser.java27;

import org.jmouse.java.parser.lexer.JavaKeywordCategory;
import org.jmouse.java.parser.lexer.JavaKeywordDefinition;

public enum Java27Keyword implements JavaKeywordDefinition {

    INSPECT(Java27Names.INSPECT),
    BUNDLE(Java27Names.BUNDLE);

    private final String spelling;

    Java27Keyword(String spelling) {
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
