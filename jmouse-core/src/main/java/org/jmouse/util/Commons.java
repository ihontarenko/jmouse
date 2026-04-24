package org.jmouse.util;

import java.util.Optional;

import static org.jmouse.core.Verify.nonNull;

final public class Commons {

    public static <T> T unwrap(Optional<T> optional, T defaultValue) {
        return nonNull(optional, "optional").orElse(defaultValue);
    }

    public static <T> T unwrap(Optional<T> optional) {
        return unwrap(optional, null);
    }

}
