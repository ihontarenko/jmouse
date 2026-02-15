package org.jmouse.validator.constraint.handler;

import org.jmouse.util.Strings;
import org.jmouse.validator.constraint.api.Constraint;

public final class ConstraintMessagePolicy {

    public String message(Constraint constraint) {
        return Strings.emptyIfNull(constraint == null ? null : constraint.message());
    }
}
