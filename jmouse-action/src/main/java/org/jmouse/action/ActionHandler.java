package org.jmouse.action;

/**
 * Handles a single action request. 🎯
 */
@FunctionalInterface
public interface ActionHandler {

    /**
     * Handles the given action request.
     *
     * @param request current action request
     * @return action result
     */
    Object handle(ActionRequest request);
}