package org.jmouse.el.language.lexer;

import org.jmouse.el.lexer.Token;

public enum LanguageToken implements Token.Type {

    T_IF(10000, "if"),
    T_ELSE_IF(10050, "else_if", "elseif"),
    T_ELSE(10100, "else"),

    T_FUNCTION(11000, "function");

    private final int      type;
    private final String[] values;

    LanguageToken(final int type, final String... values) {
        this.type = type;
        this.values = values;
    }

    @Override
    public int getTypeId() {
        return type;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> E getEnumType() {
        return (E) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> Class<E> getBundleType() {
        return (Class<E>) getEnumType().getClass();
    }

    @Override
    public String[] getTokenTemplates() {
        return values;
    }

    @Override
    public Token.Type[] getTokens() {
        return values();
    }

}
