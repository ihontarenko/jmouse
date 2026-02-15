package org.jmouse.validator.constraint.constraint;

import org.jmouse.util.Numbers;
import org.jmouse.util.Strings;
import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.api.ConstraintExecutor;

import java.math.BigDecimal;

public final class MinMaxConstraint implements Constraint {

    private Mode       mode;
    private BigDecimal min;
    private BigDecimal max;
    private String     message;

    public enum Mode { MIN, MAX, RANGE }

    @Override
    public String code() {
        return "min_max." + getMode();
    }

    @Override
    public String message() {
        return getMessage();
    }

    @Override
    public ConstraintExecutor<MinMaxConstraint> executor() {
        return Executor.INSTANCE;
    }

    private static final class Executor
            implements ConstraintExecutor<MinMaxConstraint> {

        private static final Executor INSTANCE = new Executor();

        @Override
        public boolean test(Object value, MinMaxConstraint constraint) {

            if (value == null) {
                return true;
            }

            BigDecimal decimal = Numbers.toDecimal(value);

            if (decimal == null) {
                return false;
            }

            switch (constraint.mode) {
                case MIN -> {
                    return constraint.min == null ||
                            decimal.compareTo(constraint.min) >= 0;
                }
                case MAX -> {
                    return constraint.max == null ||
                            decimal.compareTo(constraint.max) <= 0;
                }
                case RANGE -> {
                    if (constraint.min != null &&
                            decimal.compareTo(constraint.min) < 0) {
                        return false;
                    }
                    return constraint.max == null ||
                            decimal.compareTo(constraint.max) <= 0;
                }
            }

            return false;
        }

    }

    public BigDecimal getMin() {
        return min;
    }

    public void setMin(BigDecimal min) {
        this.min = min;
    }

    public BigDecimal getMax() {
        return max;
    }

    public void setMax(BigDecimal max) {
        this.max = max;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = Strings.emptyIfNull(message);
    }
}

