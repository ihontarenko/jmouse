package org.jmouse.validator.constraint.handler;

import org.jmouse.util.Strings;
import org.jmouse.validator.constraint.api.Constraint;

public final class ConstraintMessagePolicy implements ConstraintMessageProvider {

    @Override
    public String provideMessage(Constraint constraint) {
        return Strings.emptyIfNull(constraint == null ? null : constraint.message());
    }
}
