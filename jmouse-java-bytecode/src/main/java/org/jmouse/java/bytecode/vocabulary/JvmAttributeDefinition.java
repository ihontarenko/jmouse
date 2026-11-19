package org.jmouse.java.bytecode.vocabulary;

import java.util.Set;

public record JvmAttributeDefinition(
        String name,
        int major,
        int minor,
        String section,
        String node,
        Set<JvmAttributeContext> contexts,
        JvmAttributeMultiplicity multiplicity) {
    public JvmAttributeDefinition {
        contexts = Set.copyOf(contexts);
    }
}
