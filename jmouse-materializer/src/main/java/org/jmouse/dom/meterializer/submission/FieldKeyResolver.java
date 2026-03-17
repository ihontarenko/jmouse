package org.jmouse.dom.meterializer.submission;

import org.jmouse.dom.Node;

/**
 * Resolves submission keys for form fields. 🧩
 *
 * <p>
 * Used to determine how field values and validation errors
 * are addressed in submission payloads.
 * </p>
 */
public interface FieldKeyResolver {

    /**
     * Resolves the value key for the given field node.
     *
     * @param node field node
     *
     * @return key used to read/write the field value
     */
    String resolveValueKey(Node node);

    /**
     * Resolves the error key for the given field node.
     *
     * @param node field node
     *
     * @return key used to access validation errors
     */
    String resolveErrorKey(Node node);

}