package org.jmouse.web.binding;

import org.jmouse.validator.Errors;
import org.jmouse.validator.Hints;

public record BindingContext(
        Errors errors,
        String objectName,
        Hints hints
) {}
