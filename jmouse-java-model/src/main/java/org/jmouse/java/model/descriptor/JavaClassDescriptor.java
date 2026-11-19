package org.jmouse.java.model.descriptor;

import java.util.Objects;

public record JavaClassDescriptor(String internalName) implements JavaTypeDescriptor {
    public JavaClassDescriptor {
        Objects.requireNonNull(internalName, "internalName");
        if (internalName.isEmpty()
                || internalName.startsWith("/")
                || internalName.endsWith("/")
                || internalName.contains("//")
                || internalName.indexOf('.') >= 0
                || internalName.indexOf(';') >= 0
                || internalName.indexOf('[') >= 0) {
            throw new IllegalArgumentException("Invalid JVM internal class name");
        }
    }

    @Override
    public String encoding() {
        return "L" + internalName + ";";
    }
}
