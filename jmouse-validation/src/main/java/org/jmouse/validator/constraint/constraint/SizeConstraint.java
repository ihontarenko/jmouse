package org.jmouse.validator.constraint.constraint;

import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.api.ConstraintExecutor;

import java.util.Collection;
import java.util.Map;

public class SizeConstraint implements Constraint {

    @Override
    public String code() {
        return "size";
    }

    /**
     * Provides the executor responsible for evaluating this constraint.
     *
     * @return constraint executor
     */
    @Override
    public ConstraintExecutor<? extends Constraint> executor() {
        return (value, constraint) -> {
            int size = switch (value) {
                case String string -> string.length();
                case Collection<?> collection -> collection.size();
                case Map<?, ?> map -> map.size();
                default -> throw new IllegalArgumentException("Unsupported value type '%s'".formatted(
                        value.getClass()
                ));
            };

            return false;
        };
    }
}
