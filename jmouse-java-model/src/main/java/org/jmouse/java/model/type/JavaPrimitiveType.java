package org.jmouse.java.model.type;

import org.jmouse.java.model.descriptor.JavaDescriptorCodes;
import org.jmouse.java.model.descriptor.JavaTypeDescriptor;

import java.util.Locale;

public enum JavaPrimitiveType implements JavaType, JavaTypeDescriptor {
    BOOLEAN,
    BYTE,
    SHORT,
    CHAR,
    INT,
    LONG,
    FLOAT,
    DOUBLE;

    @Override
    public String displayName() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String encoding() {
        return JavaDescriptorCodes.forKeyword(displayName());
    }

    @Override
    public int parameterSlots() {
        return this == LONG || this == DOUBLE ? 2 : 1;
    }
}
