package org.jmouse.settings;

import org.jmouse.core.parameters.RequestParametersPathToken;
import org.jmouse.core.parameters.RequestParametersPathToken.ArrayIndexToken;
import org.jmouse.core.parameters.RequestParametersPathToken.PropertyNameToken;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * ✅ What a structured section's record says of its values beside their types — {@link Bounds} and {@link Colour} on its
 * components —, held against the values read into it: records, lists, maps and optionals followed down. A section is checked so
 * when it is changed, before anything is kept; a window's controls are drawn from the same marks.
 */
public final class RecordChecks {

    /** A colour as a section writes one. */
    private static final Pattern COLOUR = Pattern.compile("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?");

    /** How near a step a number may stand and still be on it: what a decimal written in text loses. */
    private static final double ON_STEP = 1e-9;

    private RecordChecks() {
    }

    /** Why a record's values are not what its marks allow — the value named by its key ({@link ParameterTree}) —; empty where every one is. */
    public static Optional<String> refusalOf(Object record) {
        return refusalIn(record, List.of());
    }

    private static Optional<String> refusalIn(Object value, List<RequestParametersPathToken> where) {
        if (value == null || !value.getClass().isRecord()) {
            return Optional.empty();
        }
        for (RecordComponent component : value.getClass().getRecordComponents()) {
            Optional<String> refusal = refusalOf(component, valueOf(component, value), deeper(where, new PropertyNameToken(component.getName())));
            if (refusal.isPresent()) {
                return refusal;
            }
        }
        return Optional.empty();
    }

    /** A component's value held against its marks, and every record within it against theirs. */
    private static Optional<String> refusalOf(RecordComponent component, Object held, List<RequestParametersPathToken> at) {
        if (held instanceof Optional<?> optional) {
            return optional.isPresent() ? refusalOf(component, optional.get(), at) : Optional.empty();
        }
        if (held instanceof Collection<?> items) {
            int place = 0;
            for (Object item : items) {
                Optional<String> refusal = refusalOf(component, item, deeper(at, new ArrayIndexToken(place)));
                if (refusal.isPresent()) {
                    return refusal;
                }
                place++;
            }
            return Optional.empty();
        }
        if (held instanceof Map<?, ?> entries) {
            for (Map.Entry<?, ?> entry : entries.entrySet()) {
                Optional<String> refusal = refusalOf(component, entry.getValue(), deeper(at, new PropertyNameToken(String.valueOf(entry.getKey()))));
                if (refusal.isPresent()) {
                    return refusal;
                }
            }
            return Optional.empty();
        }
        Bounds bounds = component.getAnnotation(Bounds.class);
        if (bounds != null && held instanceof Number number) {
            return outsideOf(bounds, number.doubleValue()).map(why -> ParameterTree.keyOf(at) + " " + why);
        }
        if (component.isAnnotationPresent(Colour.class) && held instanceof String word && !COLOUR.matcher(word).matches()) {
            return Optional.of(ParameterTree.keyOf(at) + " is a colour, written #rrggbb or #rrggbbaa: \"" + word + "\" is not one.");
        }
        return refusalIn(held, at);
    }

    /** A path one step further down. */
    private static List<RequestParametersPathToken> deeper(List<RequestParametersPathToken> path, RequestParametersPathToken step) {
        List<RequestParametersPathToken> deeper = new ArrayList<>(path);
        deeper.add(step);
        return deeper;
    }

    /** Why a number is not within its bounds or on its step; empty where it is. */
    private static Optional<String> outsideOf(Bounds bounds, double value) {
        if (value < bounds.minimum() || value > bounds.maximum()) {
            return Optional.of("runs from " + written(bounds.minimum()) + " to " + written(bounds.maximum()) + ": " + written(value) + " is outside.");
        }
        if (bounds.step() > 0) {
            double steps = (value - bounds.minimum()) / bounds.step();
            if (Math.abs(steps - Math.rint(steps)) > ON_STEP * Math.max(1, Math.abs(steps))) {
                return Optional.of("moves in steps of " + written(bounds.step()) + ": " + written(value) + " is off them.");
            }
        }
        return Optional.empty();
    }

    private static String written(double value) {
        return value == Math.rint(value) && Math.abs(value) < 1e15 ? Long.toString((long) value) : Double.toString(value);
    }

    private static Object valueOf(RecordComponent component, Object record) {
        try {
            // A section's record may be one a module keeps to itself, as its reader reads it: its accessor is opened the same way.
            Method accessor = component.getAccessor();
            accessor.trySetAccessible();
            return accessor.invoke(record);
        } catch (IllegalAccessException | InvocationTargetException unread) {
            throw new IllegalStateException("The record " + record.getClass().getName() + " does not give its " + component.getName(), unread);
        }
    }
}
