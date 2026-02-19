package org.jmouse.template.hooks;

import org.jmouse.core.Verify;
import org.jmouse.template.NodeTemplate;
import org.jmouse.template.RenderingExecution;
import org.jmouse.template.RenderingRequest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record RenderingHookChain<T>(List<RenderingHook<T>> hooks) {

    private static final Comparator<RenderingHook<?>> ORDERING =
            Comparator.<RenderingHook<?>>comparingInt(RenderingHook::order).reversed();

    public RenderingHookChain(List<RenderingHook<T>> hooks) {
        Verify.nonNull(hooks, "hooks");
        List<RenderingHook<T>> copy = new ArrayList<>(hooks);
        copy.sort(ORDERING);
        this.hooks = List.copyOf(copy);
    }

    public void beforeTemplateResolve(String key, Object data, RenderingRequest request, RenderingExecution execution) {
        for (RenderingHook<T> hook : hooks) {
            hook.beforeTemplateResolve(key, data, request, execution);
        }
    }

    public void afterTemplateResolve(String key, NodeTemplate blueprint, RenderingExecution execution) {
        for (RenderingHook<T> hook : hooks) {
            hook.afterTemplateResolve(key, blueprint, execution);
        }
    }

    public void beforeMaterialize(NodeTemplate transformed, RenderingExecution execution) {
        for (RenderingHook<T> hook : hooks) {
            hook.beforeMaterialize(transformed, execution);
        }
    }

    public void afterMaterialize(T root, RenderingExecution execution) {
        for (RenderingHook<T> hook : hooks) {
            hook.afterMaterialize(root, execution);
        }
    }

    public void onFailure(Throwable exception, RenderingStage stage, RenderingExecution execution) {
        for (RenderingHook<T> hook : hooks) {
            hook.onFailure(exception, stage, execution);
        }
    }
}
