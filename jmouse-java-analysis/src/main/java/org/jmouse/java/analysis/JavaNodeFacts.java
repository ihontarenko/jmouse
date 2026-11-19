package org.jmouse.java.analysis;

import org.jmouse.java.model.symbol.JavaSymbolKey;
import org.jmouse.java.model.type.JavaType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record JavaNodeFacts(
        JavaType type,
        Optional<JavaSymbolKey> symbol,
        Optional<JavaInvocation> invocation,
        Optional<JavaNodeIdentity> controlTarget,
        List<JavaConversion> conversions) {
    public JavaNodeFacts {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(symbol, "symbol");
        Objects.requireNonNull(invocation, "invocation");
        Objects.requireNonNull(controlTarget, "controlTarget");
        conversions = List.copyOf(conversions);
    }
}
