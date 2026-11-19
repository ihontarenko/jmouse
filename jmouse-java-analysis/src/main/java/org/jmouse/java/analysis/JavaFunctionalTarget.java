package org.jmouse.java.analysis;

import org.jmouse.java.model.symbol.JavaMethodKey;
import org.jmouse.java.model.type.JavaExecutableType;

import java.util.Objects;

/** The functional descriptor after substituting the target interface's type arguments. */
public record JavaFunctionalTarget(JavaMethodKey declaration, JavaExecutableType signature) {
    public JavaFunctionalTarget {
        Objects.requireNonNull(declaration, "declaration");
        Objects.requireNonNull(signature, "signature");
    }
}
