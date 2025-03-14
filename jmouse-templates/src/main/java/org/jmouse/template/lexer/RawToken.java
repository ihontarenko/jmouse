package org.jmouse.template.lexer;


public record RawToken(String value, int line, int offset, Type type) {

    public int length() {
        return value.length();
    }

    @Override
    public String toString() {
        return "%s:[%d:%d]: %s".formatted(type, offset, line, type == Type.RAW_TEXT ? "[LARGE]" : value);
    }

    /**
     * Defines possible types of tokens recognized by the lexer.
     */
    public enum Type {
        RAW_TEXT, OPEN_TAG, CLOSE_TAG, IDENTIFIER, NUMBER, OPERATOR, STRING, UNKNOWN
    }

}
