package org.jmouse.web.binding;

import org.jmouse.validator.Errors;
import org.jmouse.validator.ValidationHints;

public record BindingContext(
        Errors errors,
        String objectName,
        ValidationHints hints
) {}
