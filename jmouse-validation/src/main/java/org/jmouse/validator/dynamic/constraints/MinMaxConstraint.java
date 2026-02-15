package org.jmouse.validator.dynamic.constraints;

import org.jmouse.validator.dynamic.*;
import org.jmouse.validator.dynamic.ConstraintParameters.ParameterType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class MinMaxConstraint implements ConstraintDefinition {

    @Override
    public String id() {
        return "minMax";
    }

    @Override
    public List<String> aliases() {
        return List.of("min", "max", "range");
    }

    @Override
    public ConstraintParameters parameters() {
        return ConstraintParameters.builder()
                .required("mode", ParameterType.STRING)
                .optional("min", ParameterType.DECIMAL)
                .optional("max", ParameterType.DECIMAL)
                .optional("message", ParameterType.STRING)
                .build();
    }

    @Override
    public ConstraintExecutor executor() {
        return new Executor();
    }

    static final class Executor implements ConstraintExecutor {

        private static BigDecimal toDecimal(Object value) {
            if (value instanceof BigDecimal bigDecimal) {
                return bigDecimal;
            }

            if (value instanceof Number number) {
                return BigDecimal.valueOf(number.doubleValue());
            }

            try {
                return new BigDecimal(String.valueOf(value));
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public boolean test(Object value, Map<String, Object> arguments) {
            if (value == null) {
                return true;
            }

            ConstraintArguments constraintArguments = new ConstraintArguments(arguments);

            String mode = constraintArguments.string("mode");

            BigDecimal decimal = toDecimal(value);

            if (decimal == null) {
                return false;
            }

            var min = constraintArguments.decimal("min");
            var max = constraintArguments.decimal("max");

            if ("min".equalsIgnoreCase(mode)) {
                return min == null || decimal.compareTo(min) >= 0;
            }

            if ("max".equalsIgnoreCase(mode)) {
                return max == null || decimal.compareTo(max) <= 0;
            }

            if ("range".equalsIgnoreCase(mode)) {
                if (min != null && decimal.compareTo(min) < 0) {
                    return false;
                }
                return max == null || decimal.compareTo(max) <= 0;
            }

            return false;
        }
    }
}
