package org.jmouse.template.lexer;


public record RawToken(String token, int line, int offset, Type type) {

    /**
     * Defines possible types of tokens recognized by the lexer.
     */
    public enum Type {
        RAW_TEXT, OPEN_TAG, CLOSE_TAG,
        IDENTIFIER, NUMBER, OPERATOR, STRING, UNKNOWN
    }

    public int length() {
        return token.length();
    }

}
