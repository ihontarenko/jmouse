package org.jmouse.java.bytecode.grammar;

import org.jmouse.java.bytecode.vocabulary.JvmAttributeDefinition;
import org.jmouse.java.bytecode.vocabulary.JvmAttributeKind;
import org.jmouse.java.bytecode.vocabulary.JvmAttributeMultiplicity;
import org.jmouse.java.bytecode.vocabulary.JvmVocabulary;

public final class JvmStandardAttributePolicy implements JavaAttributePolicy {
    @Override
    public void validate(JavaAttributeInput input, int occurrence) {
        String name = input.pool().utf8(input.nameIndex());
        JvmAttributeKind kind = JvmAttributeKind.named(name).orElse(null);
        if (kind == null) {
            return;
        }
        JvmAttributeDefinition definition = JvmVocabulary.attribute(kind);
        if (!definition.contexts().contains(input.context())) {
            throw input.bytes()
                    .failure("Attribute " + name + " is forbidden in " + input.context());
        }
        if (definition.multiplicity() == JvmAttributeMultiplicity.SINGLE && occurrence > 1) {
            throw input.bytes().failure("Duplicate singleton attribute: " + name);
        }
    }
}
