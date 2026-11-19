package org.jmouse.java.bytecode.vocabulary;

import org.jmouse.el.lexer.Token;
import org.jmouse.java.bytecode.signature.JvmTextNames;

/** A JVM terminal usable directly by the shared token cursor. */
public interface JvmTerminalToken extends JvmToken, Token.Type {
    @Override
    default int getTypeId() {
        return JvmTextNames.TERMINAL_TOKEN_BASE + code();
    }

    @Override
    @SuppressWarnings("unchecked")
    default <E extends Enum<E>> E getEnumType() {
        return (E) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    default <E extends Enum<E>> Class<E> getBundleType() {
        return (Class<E>) getClass();
    }

    @Override
    default String[] getTokenTemplates() {
        return new String[] {Character.toString(code())};
    }
}
