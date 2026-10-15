package org.jmouse.parallax;

import java.util.Arrays;
import java.util.List;

/**
 * One knob of a computation, described well enough for the interface to draw its control and for the server to
 * refuse a value outside it: resolution 8–512, a slicing plane's offset, a choice of projection.
 */
public record SettingSpecification(String name, Type type, Object defaultValue, Double minimum, Double maximum,
                                   List<String> choices, int length, String description) {

    public enum Type { INTEGER, NUMBER, BOOLEAN, CHOICE, VECTOR, TEXT }

    public static SettingSpecification integer(String name, int defaultValue, int minimum, int maximum, String description) {
        return new SettingSpecification(name, Type.INTEGER, defaultValue, (double) minimum, (double) maximum, List.of(), 1,
                description);
    }

    public static SettingSpecification number(String name, double defaultValue, double minimum, double maximum,
                                              String description) {
        return new SettingSpecification(name, Type.NUMBER, defaultValue, minimum, maximum, List.of(), 1, description);
    }

    public static SettingSpecification flag(String name, boolean defaultValue, String description) {
        return new SettingSpecification(name, Type.BOOLEAN, defaultValue, null, null, List.of(), 1, description);
    }

    public static SettingSpecification choice(String name, Enum<?> defaultValue, String description) {
        List<String> choices = Arrays.stream(defaultValue.getDeclaringClass().getEnumConstants())
                .map(Enum::name).toList();
        return new SettingSpecification(name, Type.CHOICE, defaultValue.name(), null, null, choices, 1, description);
    }

    /** A list of numbers of `length` (0: any length, such as one initial value per state variable); an empty default
     * means "not given" and the computation decides. */
    public static SettingSpecification vector(String name, List<Double> defaultValue, int length, double minimum,
                                              double maximum, String description) {
        return new SettingSpecification(name, Type.VECTOR, defaultValue, minimum, maximum, List.of(), length, description);
    }

    public static SettingSpecification text(String name, String defaultValue, int maximumLength, String description) {
        return new SettingSpecification(name, Type.TEXT, defaultValue, null, (double) maximumLength, List.of(), 1,
                description);
    }
}
