package org.jmouse.java.bytecode.grammar;

/** Placement and multiplicity are replaceable independently from an attribute's decoder. */
@FunctionalInterface
public interface JavaAttributePolicy {
    void validate(JavaAttributeInput input, int occurrence);
}
