package org.jmouse.meterializer;

/**
 * Transforms a blueprint prior to materialization.
 */
public interface TemplateTransformer {

    /**
     * Transform a blueprint.
     *
     * @param blueprint input blueprint
     * @param execution execution context
     * @return transformed blueprint
     */
    NodeTemplate transform(NodeTemplate blueprint, RenderingExecution execution);
}
