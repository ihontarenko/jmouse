package org.jmouse.validator.dynamic;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class ConstraintArguments {

    private final Map<String, Object> arguments;

    public ConstraintArguments(Map<String, Object> arguments) {
        this.arguments = arguments == null ? Map.of() : arguments;
    }

    public String string(String key) {
        Object value = arguments.get(key);
        return value == null ? null : String.valueOf(value);
    }

    public Integer integer(String key) {
        Object value = arguments.get(key);

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    public BigDecimal decimal(String key) {
        Object value = arguments.get(key);

        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }

        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> stringList(String key) {
        Object value = arguments.get(key);

        if (value instanceof Collection<?> c) {
            return c.stream().map(String::valueOf).toList();
        }

        return null;
    }

    public Map<String, Object> raw() {
        return arguments;
    }

//    public ConstraintArguments validated(ConstraintDefinition def, ConstraintPolicy policy) {
//        return ConstraintArgumentValidator.validate(def, this, policy);
//    }
}
