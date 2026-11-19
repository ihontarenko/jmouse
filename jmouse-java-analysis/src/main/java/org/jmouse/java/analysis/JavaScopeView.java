package org.jmouse.java.analysis;

import org.jmouse.java.model.symbol.JavaSymbolKey;
import org.jmouse.java.model.symbol.JavaTypeKey;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Visibility at one source occurrence, including flow-scoped pattern bindings. Innermost layers
 * come first.
 */
public record JavaScopeView(
        List<JavaScopeLayer> layers,
        Optional<JavaTypeKey> enclosingType,
        Optional<JavaSymbolKey> enclosingExecutable) {
    public JavaScopeView {
        layers = List.copyOf(layers);
        Objects.requireNonNull(enclosingType, "enclosingType");
        Objects.requireNonNull(enclosingExecutable, "enclosingExecutable");
    }

    public List<JavaVisibleBinding> lookup(String name) {
        for (JavaScopeLayer layer : layers) {
            List<JavaVisibleBinding> matches =
                    layer.bindings().stream()
                            .filter(binding -> binding.name().equals(name))
                            .toList();
            if (!matches.isEmpty()) {
                return matches;
            }
        }
        return List.of();
    }
}
