package org.jmouse.java.parser.java27;

import org.jmouse.el.lexer.Token;

public enum Java27Token implements Token.Type {
    VALUE_PREFIX;

    @Override
    public int getTypeId() {
        return Java27Names.VALUE_TOKEN_ID;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> E getEnumType() {
        return (E) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> Class<E> getBundleType() {
        return (Class<E>) Java27Token.class;
    }

    @Override
    public String[] getTokenTemplates() {
        return new String[] {Java27Names.VALUE_PREFIX};
    }

    @Override
    public Token.Type[] getTokens() {
        return values();
    }
}
