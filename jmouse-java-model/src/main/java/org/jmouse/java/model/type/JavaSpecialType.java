package org.jmouse.java.model.type;

import java.util.Locale;

public enum JavaSpecialType implements JavaType {
    NONE,
    NULL;

    @Override
    public String displayName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
