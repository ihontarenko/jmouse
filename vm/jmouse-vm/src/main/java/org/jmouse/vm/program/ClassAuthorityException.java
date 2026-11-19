package org.jmouse.vm.program;

import org.jmouse.java.model.symbol.JavaTypeKey;

import java.util.List;

public final class ClassAuthorityException extends IllegalArgumentException {

    private final JavaTypeKey type;
    private final List<ClassDefinitionOrigin> candidates;

    public ClassAuthorityException(
            JavaTypeKey type, List<ClassDefinitionOrigin> candidates, String detail) {
        super(detail + ": " + type + "; candidates=" + candidates);
        this.type = type;
        this.candidates = List.copyOf(candidates);
    }

    public JavaTypeKey type() {
        return type;
    }

    public List<ClassDefinitionOrigin> candidates() {
        return candidates;
    }
}
