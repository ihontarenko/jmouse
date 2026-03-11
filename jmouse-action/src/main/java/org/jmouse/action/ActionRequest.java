package org.jmouse.action;

import org.jmouse.core.Verify;
import org.jmouse.core.scope.Context;

import java.util.Map;

/**
 * Runtime action request. ⚙️
 *
 * <p>
 * Combines action definition with execution context.
 * </p>
 */
public interface ActionRequest {

    /**
     * Returns action definition.
     */
    ActionDefinition definition();

    /**
     * Returns execution context.
     */
    Context context();

    /**
     * Returns action name.
     */
    default String name() {
        return definition().name();
    }

    /**
     * Returns action arguments.
     */
    default Map<String, Object> arguments() {
        return definition().arguments();
    }

    /**
     * Returns a single argument by name.
     */
    default <T> T argument(String name) {
        return definition().argument(name);
    }

    /**
     * Default {@link ActionRequest} implementation. 🧱
     */
    record Default(ActionDefinition definition, Context context) implements ActionRequest {
        public Default {
            Verify.nonNull(definition, "definition");
            Verify.nonNull(context, "context");
        }
    }

}
