package org.jmouse.dom.blueprint.hooks;

import org.jmouse.dom.Node;
import org.jmouse.dom.blueprint.RenderingExecution;
import org.jmouse.dom.blueprint.RenderingRequest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SimpleCacheHook implements RenderingHook {

    private final Map<String, Node> cache = new ConcurrentHashMap<>();

    @Override
    public int order() {
        return 1000;
    }

    @Override
    public void beforeBlueprintResolve(String blueprintKey, Object data, RenderingRequest request, RenderingExecution execution) {
        String key    = blueprintKey + ":" + data.hashCode();
        Node   cached = cache.get(key);
        if (cached != null) {
            throw new RenderingShortCircuit(cached);
        }
        execution.diagnostics().put("cacheKey", key);
    }

    @Override
    public void afterMaterialize(Node root, RenderingExecution execution) {
        Object key = execution.diagnostics().get("cacheKey");
        if (key != null) {
            cache.put(String.valueOf(key), root);
        }
    }
}
