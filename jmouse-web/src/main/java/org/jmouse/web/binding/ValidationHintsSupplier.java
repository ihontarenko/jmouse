package org.jmouse.web.binding;

import org.jmouse.validator.ValidationHints;

@FunctionalInterface
public interface ValidationHintsSupplier {
    ValidationHints get();
}