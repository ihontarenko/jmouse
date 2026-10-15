package org.jmouse.scheduler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The parameters a trigger hands its handler — a JSON object in memory.
 *
 * <p>Values are restricted to what survives a round trip through JSON: {@link String},
 * {@link Boolean}, integral numbers (held as {@link Long}), decimals (held as {@link BigDecimal}),
 * {@link List} and {@link Map} with string keys, and {@code null}. Anything else is refused at
 * construction, so a trigger that was registered is a trigger that can be stored.
 *
 * <p>The typed accessors convert between numeric representations and refuse a wrong type with the
 * parameter's name in the message: a handler reads {@code parameters.requireLong("maximumDocuments")}
 * and either gets a number or a refusal it can show.
 */
public final class TriggerParameters {

    private static final TriggerParameters EMPTY = new TriggerParameters(Map.of());

    private final Map<String, Object> values;

    private TriggerParameters(Map<String, Object> values) {
        this.values = values;
    }

    /** No parameters. */
    public static TriggerParameters empty() {
        return EMPTY;
    }

    /**
     * @param values JSON-compatible values; copied and normalised
     * @return the parameters
     * @throws IllegalArgumentException when a value has no JSON representation
     */
    public static TriggerParameters of(Map<String, ?> values) {
        Objects.requireNonNull(values, "values");

        if (values.isEmpty()) {
            return EMPTY;
        }

        return new TriggerParameters(normaliseObject(values, "parameters"));
    }

    /** An unmodifiable view, in insertion order. */
    public Map<String, Object> asMap() {
        return values;
    }

    /**
     * @param name the parameter
     * @return whether it is present (a {@code null} value counts as present)
     */
    public boolean contains(String name) {
        return values.containsKey(name);
    }

    /** The text parameter, or empty when absent or {@code null}. */
    public Optional<String> text(String name) {
        return Optional.ofNullable(typed(name, String.class, "text"));
    }

    /** The text parameter; refused when absent. */
    public String requireText(String name) {
        return text(name).orElseThrow(() -> missing(name));
    }

    /** The integral parameter, or empty when absent or {@code null}. */
    public Optional<Long> longValue(String name) {
        Object value = values.get(name);

        if (value == null) {
            return Optional.empty();
        }

        if (value instanceof Long longValue) {
            return Optional.of(longValue);
        }

        if (value instanceof BigDecimal decimal) {
            try {
                return Optional.of(decimal.longValueExact());
            } catch (ArithmeticException exception) {
                throw wrongType(name, "an integer");
            }
        }

        throw wrongType(name, "an integer");
    }

    /** The integral parameter; refused when absent. */
    public long requireLong(String name) {
        return longValue(name).orElseThrow(() -> missing(name));
    }

    /** The decimal parameter, or empty when absent or {@code null}. Integers are accepted. */
    public Optional<BigDecimal> decimal(String name) {
        Object value = values.get(name);

        if (value == null) {
            return Optional.empty();
        }

        if (value instanceof BigDecimal decimal) {
            return Optional.of(decimal);
        }

        if (value instanceof Long longValue) {
            return Optional.of(BigDecimal.valueOf(longValue));
        }

        throw wrongType(name, "a number");
    }

    /** The decimal parameter; refused when absent. */
    public BigDecimal requireDecimal(String name) {
        return decimal(name).orElseThrow(() -> missing(name));
    }

    /** The boolean parameter, or empty when absent or {@code null}. */
    public Optional<Boolean> booleanValue(String name) {
        return Optional.ofNullable(typed(name, Boolean.class, "a boolean"));
    }

    /** The boolean parameter; refused when absent. */
    public boolean requireBoolean(String name) {
        return booleanValue(name).orElseThrow(() -> missing(name));
    }

    private <T> T typed(String name, Class<T> type, String description) {
        Object value = values.get(name);

        if (value == null) {
            return null;
        }

        if (!type.isInstance(value)) {
            throw wrongType(name, description);
        }

        return type.cast(value);
    }

    private static IllegalArgumentException missing(String name) {
        return new IllegalArgumentException("The trigger parameter '" + name + "' is required.");
    }

    private static IllegalArgumentException wrongType(String name, String expected) {
        return new IllegalArgumentException("The trigger parameter '" + name + "' must be " + expected + ".");
    }

    private static Map<String, Object> normaliseObject(Map<?, ?> source, String path) {
        Map<String, Object> normalised = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new IllegalArgumentException("Trigger parameter keys are strings; '" + path + "' has "
                        + entry.getKey() + ".");
            }

            normalised.put(key, normaliseValue(entry.getValue(), path + "." + key));
        }

        return Collections.unmodifiableMap(normalised);
    }

    private static Object normaliseValue(Object value, String path) {
        return switch (value) {
            case null -> null;
            case String text -> text;
            case Boolean booleanValue -> booleanValue;
            case Long longValue -> longValue;
            case Integer integer -> integer.longValue();
            case Short shortValue -> shortValue.longValue();
            case Byte byteValue -> byteValue.longValue();
            case java.math.BigInteger bigInteger -> normaliseDecimal(new BigDecimal(bigInteger));
            case BigDecimal decimal -> normaliseDecimal(decimal);
            case Double doubleValue -> normaliseFloating(doubleValue, path);
            case Float floatValue -> normaliseFloating(floatValue.doubleValue(), path);
            case Map<?, ?> map -> normaliseObject(map, path);
            case List<?> list -> normaliseList(list, path);
            default -> throw new IllegalArgumentException("The trigger parameter '" + path + "' has no JSON form: "
                    + value.getClass().getName() + ".");
        };
    }

    private static Object normaliseFloating(double value, String path) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("The trigger parameter '" + path + "' is not a finite number.");
        }

        return normaliseDecimal(BigDecimal.valueOf(value));
    }

    /** Whole decimals become {@link Long}, so {@code 5} and {@code 5.0} read back the same. */
    private static Object normaliseDecimal(BigDecimal decimal) {
        BigDecimal stripped = decimal.stripTrailingZeros();

        if (stripped.scale() <= 0 && stripped.toBigInteger().bitLength() < 64) {
            return stripped.longValueExact();
        }

        return stripped;
    }

    private static List<Object> normaliseList(List<?> list, String path) {
        List<Object> normalised = new ArrayList<>(list.size());

        for (int index = 0; index < list.size(); index++) {
            normalised.add(normaliseValue(list.get(index), path + "[" + index + "]"));
        }

        return Collections.unmodifiableList(normalised);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TriggerParameters that && values.equals(that.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
