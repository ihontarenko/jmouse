package org.jmouse.vm.program;

import org.jmouse.java.model.symbol.JavaTypeKey;

import java.util.List;

/** Chooses one entire class candidate; cannot assemble a class from members of different inputs. */
@FunctionalInterface
public interface ClassAuthority {

    ClassDefinitionOrigin select(JavaTypeKey type, List<ClassDefinitionOrigin> candidates);

    static ClassAuthority unique() {
        return (type, candidates) -> {
            if (candidates.size() != 1) {
                throw new ClassAuthorityException(
                        type,
                        candidates,
                        "Multiple class definitions require an explicit authority policy");
            }
            return candidates.getFirst();
        };
    }
}
