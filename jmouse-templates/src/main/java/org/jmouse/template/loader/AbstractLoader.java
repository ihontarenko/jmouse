package org.jmouse.template.loader;

abstract public class AbstractLoader<T> implements TemplateLoader<T> {

    private String prefix;
    private String suffix;
    private String charset;

    @Override
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    @Override
    public void setCharset(String charset) {
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
    public String getCharset() {
        return this.charset;
    }
}
