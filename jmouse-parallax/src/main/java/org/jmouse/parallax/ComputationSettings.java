package org.jmouse.parallax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Settings checked against a computation's specification: every key known, every number in range, every choice
 * one of its options, defaults filled in. What a computation reads can therefore never be out of bounds, and the
 * resolved map is what goes into the cache fingerprint.
 */
public final class ComputationSettings {

    private final Map<String, Object> values;

    private ComputationSettings(Map<String, Object> values) {
        this.values = Collections.unmodifiableMap(values);
    }

    public static ComputationSettings resolve(List<SettingSpecification> specifications, Map<String, Object> given) {
        Map<String, Object> raw = given == null ? Map.of() : given;
        Map<String, SettingSpecification> byName = new LinkedHashMap<>();
        specifications.forEach(specification -> byName.put(specification.name(), specification));
        for (String key : raw.keySet()) {
            if (!byName.containsKey(key)) {
                throw new IllegalArgumentException("'" + key + "' is not a setting of this representation. Known: "
                        + String.join(", ", byName.keySet()) + ".");
            }
        }
        Map<String, Object> resolved = new TreeMap<>();
        for (SettingSpecification specification : specifications) {
            Object value = raw.containsKey(specification.name()) && raw.get(specification.name()) != null
                    ? raw.get(specification.name()) : specification.defaultValue();
            resolved.put(specification.name(), coerce(specification, value));
        }
        return new ComputationSettings(resolved);
    }

    private static Object coerce(SettingSpecification specification, Object value) {
        String name = specification.name();
        return switch (specification.type()) {
            case INTEGER -> {
                double number = number(name, value);
                if (number != Math.rint(number)) {
                    throw new IllegalArgumentException("'" + name + "' is a whole number.");
                }
                yield (int) within(specification, number);
            }
            case NUMBER -> within(specification, number(name, value));
            case BOOLEAN -> {
                if (!(value instanceof Boolean flag)) {
                    throw new IllegalArgumentException("'" + name + "' is true or false.");
                }
                yield flag;
            }
            case CHOICE -> {
                String choice = String.valueOf(value);
                if (!specification.choices().contains(choice)) {
                    throw new IllegalArgumentException("'" + name + "' is one of " + specification.choices() + ".");
                }
                yield choice;
            }
            case VECTOR -> {
                if (!(value instanceof List<?> list)) {
                    throw new IllegalArgumentException("'" + name + "' is a list of numbers.");
                }
                if (specification.length() > 0 && !list.isEmpty() && list.size() != specification.length()) {
                    throw new IllegalArgumentException("'" + name + "' has " + specification.length() + " numbers.");
                }
                List<Double> numbers = new ArrayList<>();
                for (Object element : list) {
                    numbers.add(within(specification, number(name, element)));
                }
                yield List.copyOf(numbers);
            }
            case TEXT -> {
                String text = String.valueOf(value);
                if (specification.maximum() != null && text.length() > specification.maximum()) {
                    throw new IllegalArgumentException("'" + name + "' is too long.");
                }
                yield text;
            }
        };
    }

    private static double number(String name, Object value) {
        if (value instanceof Number number && Double.isFinite(number.doubleValue())) {
            return number.doubleValue();
        }
        throw new IllegalArgumentException("'" + name + "' is a finite number.");
    }

    private static double within(SettingSpecification specification, double value) {
        if ((specification.minimum() != null && value < specification.minimum())
                || (specification.maximum() != null && value > specification.maximum())) {
            throw new IllegalArgumentException("'" + specification.name() + "' is between " + specification.minimum()
                    + " and " + specification.maximum() + ".");
        }
        return value;
    }

    public int integer(String name) {
        return (Integer) require(name);
    }

    public double number(String name) {
        return (Double) require(name);
    }

    public boolean flag(String name) {
        return (Boolean) require(name);
    }

    public <E extends Enum<E>> E choice(String name, Class<E> type) {
        return Enum.valueOf(type, (String) require(name));
    }

    @SuppressWarnings("unchecked")
    public List<Double> vector(String name) {
        return (List<Double>) require(name);
    }

    public double[] vectorOr(String name, double[] fallback) {
        List<Double> vector = vector(name);
        return vector.isEmpty() ? fallback : vector.stream().mapToDouble(Double::doubleValue).toArray();
    }

    public String text(String name) {
        return (String) require(name);
    }

    /** The resolved settings in key order: what the cache fingerprint is computed over. */
    public Map<String, Object> asMap() {
        return values;
    }

    private Object require(String name) {
        Object value = values.get(name);
        if (value == null) {
            throw new IllegalStateException("The computation reads '" + name + "', which it does not declare.");
        }
        return value;
    }
}
