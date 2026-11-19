package org.jmouse.java.parser.extension.fixture;

import org.jmouse.el.lexer.Token;

enum FixtureToken implements Token.Type {
    TRACE_PREFIX;

    @Override
    public int getTypeId() {
        return FixtureNames.TRACE_TOKEN_ID;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> E getEnumType() {
        return (E) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> Class<E> getBundleType() {
        return (Class<E>) FixtureToken.class;
    }

    @Override
    public String[] getTokenTemplates() {
        return new String[]{FixtureNames.TRACE_PREFIX};
    }

    @Override
    public Token.Type[] getTokens() {
        return values();
    }
}
