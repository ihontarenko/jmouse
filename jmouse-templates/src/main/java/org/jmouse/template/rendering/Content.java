package org.jmouse.template.rendering;

public interface Content {

    void prepend(char[] data);

    void append(char[] data);

    char[] getContent();

    default int length() {
        return getContent().length;
    }

    default void append(String data) {
        append(data.toCharArray());
    }

    default void prepend(String data) {
        prepend(data.toCharArray());
    }

    default void append(Content content) {
        append(content.getContent());
    }

    default void prepend(Content content) {
        prepend(content.getContent());
    }

}
