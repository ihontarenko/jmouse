package org.jmouse.dom.template.hooks;

import org.jmouse.dom.Node;
import org.jmouse.dom.template.NodeTemplate;
import org.jmouse.dom.template.RenderingExecution;
import org.jmouse.dom.template.RenderingRequest;

/**
 * Intercepts the rendering pipeline.
 */
public interface RenderingHook {

    /**
     * @return hook ordering. Higher value means higher precedence.
     */
    default int order() {
        return 0;
    }

    /**
     * Called before blueprint resolving.
     */
    default void beforeBlueprintResolve(String blueprintKey, Object data, RenderingRequest request, RenderingExecution execution) {
    }

    /**
     * Called after blueprint resolving.
     */
    default void afterBlueprintResolve(String blueprintKey, NodeTemplate blueprint, RenderingExecution execution) {
    }

    /**
     * Called before materialization.
     */
    default void beforeMaterialize(NodeTemplate transformed, RenderingExecution execution) {
    }

    /**
     * Called after materialization.
     */
    default void afterMaterialize(Node root, RenderingExecution execution) {
    }

    /**
     * Called when an exception happens in any stage.
     */
    default void onFailure(Throwable exception, RenderingStage stage, RenderingExecution execution) {
    }
}
