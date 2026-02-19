package org.jmouse.template.hooks;

import org.jmouse.core.Verify;
import org.jmouse.dom.Node;
import org.jmouse.template.NodeTemplate;
import org.jmouse.template.RenderingExecution;
import org.jmouse.template.RenderingRequest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Applies ordered hooks around pipeline stages.
 */
public record RenderingHookChain(List<RenderingHook> hooks) {

    private static final Comparator<RenderingHook> ORDERING =
            Comparator.comparingInt(RenderingHook::order).reversed();

    public RenderingHookChain(List<RenderingHook> hooks) {
        Verify.nonNull(hooks, "hooks");
        List<RenderingHook> copy = new ArrayList<>(hooks);
        copy.sort(ORDERING);
        this.hooks = List.copyOf(copy);
    }

    public void beforeTemplateResolve(String key, Object data, RenderingRequest request, RenderingExecution execution) {
        for (RenderingHook hook : hooks) {
            hook.beforeTemplateResolve(key, data, request, execution);
        }
    }

    public void afterTemplateResolve(String key, NodeTemplate blueprint, RenderingExecution execution) {
        for (RenderingHook hook : hooks) {
            hook.afterTemplateResolve(key, blueprint, execution);
        }
    }

    public void beforeMaterialize(NodeTemplate transformed, RenderingExecution execution) {
        for (RenderingHook hook : hooks) {
            hook.beforeMaterialize(transformed, execution);
        }
    }

    public void afterMaterialize(Node root, RenderingExecution execution) {
        for (RenderingHook hook : hooks) {
            hook.afterMaterialize(root, execution);
        }
    }

    public void onFailure(Throwable exception, RenderingStage stage, RenderingExecution execution) {
        for (RenderingHook hook : hooks) {
            hook.onFailure(exception, stage, execution);
        }
    }
}
