package org.jmouse.vm.executable;

import org.jmouse.vm.program.ExecutableMethodKey;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable physical method bindings. Project customization produces another snapshot. */
public final class ExecutableBodyBindings {

    private final Map<ExecutableMethodKey, ExecutableMethodBinding> bindings;

    private ExecutableBodyBindings(Map<ExecutableMethodKey, ExecutableMethodBinding> bindings) {
        this.bindings = Map.copyOf(bindings);
    }

    public static ExecutableBodyBindings of(Collection<ExecutableMethodBinding> registrations) {
        Map<ExecutableMethodKey, ExecutableMethodBinding> bindings = new HashMap<>();
        for (ExecutableMethodBinding registration : registrations) {
            if (bindings.putIfAbsent(registration.method(), registration) != null) {
                throw new IllegalArgumentException(
                        "Duplicate method implementation binding: " + registration.method());
            }
        }
        return new ExecutableBodyBindings(bindings);
    }

    public Collection<ExecutableMethodBinding> bindings() {
        return bindings.values();
    }

    public Optional<ExecutableMethodBinding> find(ExecutableMethodKey method) {
        return Optional.ofNullable(bindings.get(Objects.requireNonNull(method, "method")));
    }

    public ExecutableMethodBinding require(ExecutableMethodKey method) {
        return find(method).orElseThrow(() -> new MissingExecutableBodyException(method));
    }

    public ExecutableBodyBindings withBinding(ExecutableMethodBinding binding) {
        Objects.requireNonNull(binding, "binding");
        Map<ExecutableMethodKey, ExecutableMethodBinding> added = new HashMap<>(bindings);
        if (added.putIfAbsent(binding.method(), binding) != null) {
            throw new IllegalArgumentException(
                    "Method implementation already bound: " + binding.method());
        }
        return new ExecutableBodyBindings(added);
    }

    public ExecutableBodyBindings withReplacement(
            ExecutableMethodKey method,
            ImplementationIdentity expected,
            ExecutableMethodBinding replacement) {
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(replacement, "replacement");
        ExecutableMethodBinding current = require(method);
        if (!method.equals(replacement.method())) {
            throw new IllegalArgumentException(
                    "A replacement must preserve the physical method identity");
        }
        if (!current.identity().equals(expected)) {
            throw new ImplementationBindingException(expected, current.identity());
        }
        Map<ExecutableMethodKey, ExecutableMethodBinding> replaced = new HashMap<>(bindings);
        replaced.put(method, replacement);
        return new ExecutableBodyBindings(replaced);
    }
}
