package org.jmouse.java.model.type;

import java.util.Objects;

public record JavaArrayType(JavaType element) implements JavaType {
    public JavaArrayType {
        Objects.requireNonNull(element, "element");
        if (element == JavaVoidType.VOID
                || element instanceof JavaSpecialType
                || element instanceof JavaExecutableType
                || element instanceof JavaWildcardType) {
            throw new IllegalArgumentException("This type cannot be an array element: " + element);
        }
    }

    @Override
    public String displayName() {
        return element.displayName() + "[]";
    }
}
