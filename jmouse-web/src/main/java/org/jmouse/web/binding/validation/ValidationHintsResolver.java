package org.jmouse.web.binding.validation;

import org.jmouse.validator.ValidationHints;

public interface ValidationHintsResolver {

    ValidationHints resolve(Class<?>[] groups);

    static ValidationHintsResolver defaults() {
        return groups -> groups == null || groups.length == 0
                ? ValidationHints.empty() : ValidationHints.of(groups);
    }
}
