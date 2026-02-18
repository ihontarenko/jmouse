package org.jmouse.dom.blueprint;

/**
 * Transforms a blueprint prior to materialization.
 */
public interface BlueprintTransformer {

    /**
     * Transform a blueprint.
     *
     * @param blueprint input blueprint
     * @param execution execution context
     * @return transformed blueprint
     */
    Blueprint transform(Blueprint blueprint, RenderingExecution execution);
}
