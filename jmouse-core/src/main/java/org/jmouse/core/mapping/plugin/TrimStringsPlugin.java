package org.jmouse.core.mapping.plugin;

public final class TrimStringsPlugin implements MappingPlugin {

    @Override
    public Object onValue(MappingValue value) {
        Object current = value.current();

        if (current instanceof String string) {
            current = string.trim().toUpperCase();
            System.out.println(value.targetType());
        }

        return current;
    }

}