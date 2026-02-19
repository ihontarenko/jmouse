package org.jmouse.dom.template.standard;

public enum InputType {

    TEXT("text"),
    NUMBER("number"),
    EMAIL("email"),
    PASSWORD("password"),
    DATE("date"),
    URL("url");

    private final String htmlValue;

    InputType(String htmlValue) {
        this.htmlValue = htmlValue;
    }

    public String htmlValue() {
        return htmlValue;
    }
}
