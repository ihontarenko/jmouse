package org.jmouse.dom.blueprint.hooks;

import org.jmouse.dom.blueprint.RenderingExecution;
import org.jmouse.dom.blueprint.RenderingRequest;

public final class DefaultFormAttributesHook implements RenderingHook {

    private final String method;
    private final String action;

    public DefaultFormAttributesHook(String method, String action) {
        this.method = method;
        this.action = action;
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public void beforeBlueprintResolve(String blueprintKey, Object data, RenderingRequest request, RenderingExecution execution) {
        request.attribute("method", getMethod());
        request.attribute("action", getAction());
    }

    public String getMethod() {
        return method;
    }

    public String getAction() {
        return action;
    }
}
