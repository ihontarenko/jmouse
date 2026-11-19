package org.jmouse.java.bytecode.signature;

import org.jmouse.el.lexer.Token;

public enum JvmTextToken implements Token.Type {
    CHARACTER(JvmTextNames.CHARACTER_TOKEN),
    END(JvmTextNames.END_TOKEN);

    private final int identity;

    JvmTextToken(int identity) {
        this.identity = identity;
    }

    @Override
    public int getTypeId() {
        return identity;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> E getEnumType() {
        return (E) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> Class<E> getBundleType() {
        return (Class<E>) JvmTextToken.class;
    }

    @Override
    public String[] getTokenTemplates() {
        return new String[0];
    }

    @Override
    public JvmTextToken[] getTokens() {
        return values();
    }
}
