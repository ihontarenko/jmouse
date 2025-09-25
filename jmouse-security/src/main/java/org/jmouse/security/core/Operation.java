package org.jmouse.security.core;

import java.util.Map;

/**
 * 🎬 Operation to be authorized.
 *
 * <p>Represents an action verb (e.g. {@code "READ"}, {@code "WRITE"},
 * {@code "HTTP:GET"}, {@code "RPC:CALL"}) plus optional attributes.</p>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * Operation readOp = new Operation() {
 *     @Override
 *     public String verb() {
 *         return "READ";
 *     }
 *
 *     @Override
 *     public Map<Object, Object> attributes() {
 *         return Map.of("resourceType", "document");
 *     }
 * };
 *
 * System.out.println(readOp.verb());        // READ
 * System.out.println(readOp.attributes());  // {resourceType=document}
 * }</pre>
 */
public interface Operation {

    /**
     * 🏷️ Verb describing the action
     * (e.g. "READ", "WRITE", "INVOKE", "HTTP:GET").
     *
     * @return operation verb
     */
    String verb();

    /**
     * 📦 Extra attributes for the operation
     * (parameters, mode, metadata).
     *
     * @return attribute map
     */
    Map<Object, Object> attributes();
}
