package org.jmouse.web.binding.validation;

import org.jmouse.validator.Hints;

public interface ValidationHintsResolver {

    Hints resolve(Class<?>[] groups);

    static ValidationHintsResolver defaults() {
        return groups -> groups == null || groups.length == 0
                ? Hints.empty() : Hints.of(groups);
    }
}
