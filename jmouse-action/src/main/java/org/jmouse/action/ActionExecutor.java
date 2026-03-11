package org.jmouse.action;

import org.jmouse.core.scope.Context;

import static org.jmouse.core.Verify.nonNull;

/**
 * Executes actions. ▶️
 *
 * <p>
 * An {@code ActionExecutor} is responsible for resolving an {@link ActionHandler}
 * from an {@link ActionRegistry} and invoking it using the provided
 * {@link ActionDefinition} or {@link ActionRequest}.
 * </p>
 */
public interface ActionExecutor {

    /**
     * Executes the given action definition in the provided context.
     *
     * <p>
     * This method creates an {@link ActionRequest} internally and delegates
     * to {@link #execute(ActionRequest)}.
     * </p>
     *
     * @param definition action definition
     * @param context execution context
     * @param <T> expected result type
     * @return action result
     */
    <T> T execute(ActionDefinition definition, Context context);

    /**
     * Executes the given action request.
     *
     * @param request action request
     * @param <T> expected result type
     * @return action result
     */
    <T> T execute(ActionRequest request);

    static ActionExecutor defaults(ActionRegistry registry) {
        return new Default(registry);
    }

    /**
     * Default {@link ActionExecutor} implementation backed by an {@link ActionRegistry}. ⚙️
     *
     * <p>
     * Resolves {@link ActionDescriptor} by action name and delegates execution
     * to the associated {@link ActionHandler}.
     * </p>
     */
    class Default implements ActionExecutor {

        /**
         * Action registry used for handler lookup.
         */
        private final ActionRegistry registry;

        /**
         * Creates executor using the given registry.
         *
         * @param registry action registry
         */
        public Default(ActionRegistry registry) {
            this.registry = nonNull(registry, "registry");
        }

        /**
         * Executes the action definition by creating a request wrapper.
         */
        @Override
        public <T> T execute(ActionDefinition definition, Context context) {
            return execute(new ActionRequest.Default(
                    nonNull(definition, "definition"),
                    nonNull(context, "context")
            ));
        }

        /**
         * Resolves the action handler and executes it.
         *
         * <p>
         * Any non-{@link ActionException} errors are wrapped into
         * {@link ActionExecutionException}.
         * </p>
         */
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

}