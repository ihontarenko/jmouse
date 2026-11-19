package org.jmouse.java.model.descriptor;

import org.jmouse.java.model.type.JavaVoidType;

import java.util.Objects;

public record JavaArrayDescriptor(JavaTypeDescriptor element, int dimensions)
        implements JavaTypeDescriptor {
    public static final int MAXIMUM_DIMENSIONS = 255;

    public JavaArrayDescriptor {
        Objects.requireNonNull(element, "element");
        if (element == JavaVoidType.VOID
                || element instanceof JavaArrayDescriptor
                || dimensions < 1
                || dimensions > MAXIMUM_DIMENSIONS) {
            throw new IllegalArgumentException(
                    "An array requires a non-array value element and a valid rank");
        }
    }

    @Override
    public String encoding() {
        return "[".repeat(dimensions) + element.encoding();
    }
}
