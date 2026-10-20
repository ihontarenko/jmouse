package org.jmouse.parallax.component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A component's text parameter: the words it holds unless a part writes others, and — when it declares them — the only
 * words a part may write: {@code operation = "write" in ["write", "read", "write-read"]}.
 *
 * @param value       the words, unquoted
 * @param choices     what a part may write over it; empty for any text
 * @param description what it is, as the document says; empty when it says nothing
 */
public record TextParameter(String value, List<String> choices, String description) {

    public TextParameter {
        choices = List.copyOf(choices);
        description = description == null ? "" : description;
        if (!choices.isEmpty() && !choices.contains(value)) {
            throw new IllegalArgumentException("\"" + value + "\" is not one of its choices " + listed(choices));
        }
    }

    /**
     * The words a part writes over it, when they are among its choices.
     *
     * @throws IllegalArgumentException naming the choices, when they are not
     */
    public String accepted(String words, String name) {
        if (!choices.isEmpty() && !choices.contains(words)) {
            throw new IllegalArgumentException("its " + name + " is one of " + listed(choices) + ", not \"" + words + "\"");
        }
        return words;
    }

    private static String listed(List<String> choices) {
        return choices.stream().map(choice -> "\"" + choice + "\"").collect(Collectors.joining(", ", "[", "]"));
    }
}
