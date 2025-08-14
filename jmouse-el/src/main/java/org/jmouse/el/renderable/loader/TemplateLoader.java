package org.jmouse.el.renderable.loader;

import org.jmouse.core.Charset;

import java.io.Reader;

public interface TemplateLoader<T> {

    Reader load(T key);

    default void setPrefix(String prefix) {

    }

    default void setSuffix(String suffix) {

    }

    default void setCharset(Charset charset) {

    }

    default String getPrefix() {
        return "";
    }

    default String getSuffix() {
        return "";
    }

    default Charset getCharset() {
        return Charset.UTF_8;
    }

}
