package org.jmouse.meterializer;

public record ViewBlock(String templateKey, Object model) {

    public static ViewBlock of(String templateKey, Object model) {
        return new ViewBlock(templateKey, model);
    }

}