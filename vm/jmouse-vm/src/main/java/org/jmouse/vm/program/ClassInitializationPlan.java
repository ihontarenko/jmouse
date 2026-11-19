package org.jmouse.vm.program;

import org.jmouse.java.model.symbol.JavaTypeKey;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Ordered prerequisites; execution guards and erroneous-initialization state belong to runtime. */
public record ClassInitializationPlan(
        JavaTypeKey type,
        List<JavaTypeKey> prerequisites,
        Optional<ExecutableMethodKey> initializer) {

    public ClassInitializationPlan {
        Objects.requireNonNull(type, "type");
        prerequisites = List.copyOf(prerequisites);
        Objects.requireNonNull(initializer, "initializer");
        if (prerequisites.contains(type)
                || prerequisites.stream().distinct().count() != prerequisites.size()
                || initializer.filter(method -> !method.owner().equals(type)).isPresent()) {
            throw new IllegalArgumentException("Invalid class initialization plan for " + type);
        }
    }
}
