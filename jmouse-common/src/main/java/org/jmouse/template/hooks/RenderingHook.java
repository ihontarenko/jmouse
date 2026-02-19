package org.jmouse.template.hooks;

import org.jmouse.dom.Node;
import org.jmouse.template.NodeTemplate;
import org.jmouse.template.RenderingExecution;
import org.jmouse.template.RenderingRequest;

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
    default void beforeTemplateResolve(String blueprintKey, Object data, RenderingRequest request, RenderingExecution execution) {
    }

    /**
     * Called after blueprint resolving.
     */
    default void afterTemplateResolve(String blueprintKey, NodeTemplate blueprint, RenderingExecution execution) {
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
