package org.jmouse.el.renderable.loader;

import org.jmouse.util.Charset;

abstract public class AbstractLoader<T> implements TemplateLoader<T> {

    private String  prefix;
    private String  suffix;
    private Charset charset;

    @Override
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    @Override
    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    @Override
    public String getPrefix() {
        return this.prefix;
    }

    @Override
    public String getSuffix() {
        return this.suffix;
    }

    @Override
    public Charset getCharset() {
        return this.charset;
    }
}
