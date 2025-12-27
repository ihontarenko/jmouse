package org.jmouse.jdbc.parameters;

import org.jmouse.el.lexer.Token;

public enum SQLParameterToken implements Token.Type {

    T_TEXT(1),
    T_NAMED_PARAMETER(2),       // ":name" or ":name|upper|trim"
    T_POSITIONAL_PARAMETER(3),  // "?"
    T_STRING(4),                // '...' or "..."
    T_LINE_COMMENT(5),          // -- ...
    T_BLOCK_COMMENT(6);         // /* ... */

    private final int id;

    SQLParameterToken(int id) {
        this.id = id;
    }

    @Override
    public int getTypeId() {
        return id;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> E getEnumType() {
        return (E) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> Class<E> getBundleType() {
        return (Class<E>) SQLParameterToken.class;
    }

    @Override
    public String[] getTokenTemplates() {
        return new String[0];
    }

    @Override
    public Token.Type[] getTokens() {
        return values();
    }

}
