package org.jmouse.web.binding;

import org.jmouse.validator.Errors;
import org.jmouse.validator.ValidationHints;

public final class BindingSession {

    private final Errors                  errors;
    private final String                  objectName;
    private final ValidationHintsSupplier hints;

    public BindingSession(Errors errors, String objectName, ValidationHintsSupplier hints) {
        this.errors = errors;
        this.objectName = objectName;
        this.hints = hints;
    }

    public Errors errors() {
        return errors;
    }

    public String objectName() {
        return objectName;
    }

    public ValidationHints hints() {
        return hints == null ? ValidationHints.empty() : hints.get();
    }
}
