package org.jmouse.vm.executable;

import org.jmouse.java.model.symbol.JavaTypeKey;
import org.jmouse.vm.program.ExecutableMethodKey;

import java.util.Set;

public final class ClassReplacementConflictException extends IllegalArgumentException {

    private final JavaTypeKey type;
    private final Set<ExecutableMethodKey> methodReplacements;

    public ClassReplacementConflictException(
            JavaTypeKey type, Set<ExecutableMethodKey> methodReplacements) {
        super(
                "Class replacement would discard explicit method replacements for "
                        + type
                        + ": "
                        + methodReplacements);
        this.type = type;
        this.methodReplacements = Set.copyOf(methodReplacements);
    }

    public JavaTypeKey type() {
        return type;
    }

    public Set<ExecutableMethodKey> methodReplacements() {
        return methodReplacements;
    }
}
