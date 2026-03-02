package org.jmouse.web.binding;

import org.jmouse.validator.Hints;

@FunctionalInterface
public interface ValidationHintsSupplier {
    Hints get();
}