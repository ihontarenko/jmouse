package org.jmouse.action;

import org.jmouse.core.scope.Context;

import static org.jmouse.core.Verify.nonNull;

/**
 * Default {@link ActionExecutor} implementation. 🧱
 */
public class DefaultActionExecutor implements ActionExecutor {

    private final ActionRegistry registry;

    public DefaultActionExecutor(ActionRegistry registry) {
        this.registry = nonNull(registry, "registry");
    }

    @Override
    public <T> T execute(ActionDefinition definition, Context context) {
        return execute(new ActionRequest.Default(
                nonNull(definition, "definition"), nonNull(context, "context")
        ));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T execute(ActionRequest request) {
        nonNull(request, "request");

        ActionDescriptor descriptor = registry.getDescriptor(request.name());

        try {
            return (T) descriptor.handler().handle(request);
        } catch (ActionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ActionExecutionException(
                    "Failed to execute action '%s'.".formatted(request.name()),
                    exception
            );
        }
    }
}