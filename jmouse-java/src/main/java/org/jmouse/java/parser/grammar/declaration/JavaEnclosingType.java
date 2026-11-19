package org.jmouse.java.parser.grammar.declaration;

import java.util.Objects;

public record JavaEnclosingType(String name, JavaTypeForm form) {
    public JavaEnclosingType {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(form, "form");
    }
}
