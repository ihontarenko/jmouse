package org.jmouse.security._old;

import java.util.Map;

/**
 * 🔐 Immutable {@link Operation} implementation.
 *
 * <p>Represents a security-relevant action (verb + attributes).</p>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * Operation readOp = SecurityOperation.of(
 *     "READ",
 *     Map.of("resourceType", "document", "sensitivity", "high")
 * );
 *
 * System.out.println(readOp.verb());        // READ
 * System.out.println(readOp.attributes());  // {resourceType=document, sensitivity=high}
 * }</pre>
 *
 * @param verb       operation verb ("READ", "WRITE", "INVOKE", "HTTP:GET", ...)
 * @param attributes extra attributes (parameters, metadata, flags)
 */
public record SecurityOperation(String verb, Map<Object, Object> attributes) implements Operation {

    /**
     * 🏗️ Factory method for creating an {@link Operation}.
     *
     * @param verb       action verb
     * @param attributes extra attributes
     * @return immutable operation
     */
    public static Operation of(String verb, Map<Object, Object> attributes) {
        return new SecurityOperation(verb, attributes);
    }

    /**
     * 🏷️ Operation verb.
     *
     * <p>Examples: "READ", "WRITE", "INVOKE", "HTTP:GET", "RPC:CALL".</p>
     */
    @Override
    public String verb() {
        return verb;
    }
}
