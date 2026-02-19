package org.jmouse.template.defaults;

public enum InputType {

    TEXT("text"),
    NUMBER("number"),
    EMAIL("email"),
    PASSWORD("password"),
    DATE("date"),
    URL("url"),

    BUTTON("button"),
    SUBMIT("submit");

    private final String htmlValue;

    InputType(String htmlValue) {
        this.htmlValue = htmlValue;
    }

    public String htmlValue() {
        return htmlValue;
    }
}
