package org.jmouse.dom.template.model;

import java.util.List;

/**
 * Stable form contract designed for blueprint-driven rendering.
 *
 * <p>This model is intentionally independent from any client DTO or database entities.</p>
 *
 * <p>Key idea:</p>
 * <ul>
 *   <li>{@link FormModel} is a container of nodes (not "fields only")</li>
 *   <li>submit buttons are regular nodes</li>
 *   <li>submission state is represented by {@link ControlState} and is not required to exist in client DTO</li>
 * </ul>
 */
public record FormModel(FormMetadata metadata, List<FormNodeModel> content) {
    public static FormModel of(FormMetadata metadata, List<FormNodeModel> content) {
        return new FormModel(metadata, content);
    }
}
