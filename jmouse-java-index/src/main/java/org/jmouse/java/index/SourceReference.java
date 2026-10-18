package org.jmouse.java.index;

import org.jmouse.java.parser.model.SymbolId;
import org.jmouse.java.parser.source.SourceSpan;

/**
 * One written occurrence, not a resolved graph edge. Its span always addresses original UTF-16
 * text.
 */
public record SourceReference(
        Role role,
        Origin origin,
        String target,
        SymbolId owner,
        SymbolId scopeType,
        SourceSpan span,
        boolean reliable) {
    public enum Origin {
        HEADER,
        IMPORT,
        DOCUMENTATION,
        BODY
    }

    public enum Role {
        EXTENDS,
        IMPLEMENTS,
        PERMITS,
        FIELD_TYPE,
        RETURN_TYPE,
        PARAMETER_TYPE,
        RECORD_COMPONENT_TYPE,
        THROWS,
        TYPE_PARAMETER_BOUND,
        TYPE_ARGUMENT,
        ANNOTATION,
        IMPORTS,
        REFERENCES,
        CALLS,
        READS,
        WRITES,
        READS_WRITES,
        INSTANTIATES,
        METHOD_REFERENCE
    }
}
