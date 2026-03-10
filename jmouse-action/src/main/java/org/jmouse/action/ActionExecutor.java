package org.jmouse.action;

import org.jmouse.core.scope.Context;

/**
 * Executes actions. ▶️
 */
public interface ActionExecutor {

    /**
     * Executes the given action definition in the provided context.
     *
     * @param definition action definition
     * @param context execution context
     * @return action result
     */
    <T> T execute(ActionDefinition definition, Context context);

    /**
     * Executes the given action request.
     *
     * @param request action request
     * @return action result
     */
    <T> T execute(ActionRequest request);

}