package org.jmouse.template.loader;

import java.io.Reader;

public interface TemplateLoader<T> {

    Reader load(T key);

    default void setPrefix(String prefix) {

    }

    default void setSuffix(String suffix) {

    }

    default void setCharset(String charset) {

    }

    default String getPrefix() {
        return null;
    }

    default String getSuffix() {
        return null;
    }

    default String getCharset() {
        return null;
    }

}
