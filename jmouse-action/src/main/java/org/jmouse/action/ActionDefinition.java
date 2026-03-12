package org.jmouse.action;

import org.jmouse.core.Verify;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.jmouse.core.Verify.nonNull;
import static org.jmouse.core.Verify.notBlank;

/**
 * Declarative action description. 🧩
 *
 * <p>
 * Defines the target action name and raw argument values
 * that should be passed to the action at execution time.
 * </p>
 */
public interface ActionDefinition {

    /**
     * Returns the unique action name.
     *
     * <p>Example: {@code config.process}</p>
     */
    String name();

    /**
     * Returns raw action arguments.
     */
    Map<String, Object> arguments();

    /**
     * Returns a single argument by name.
     */
    @SuppressWarnings("unchecked")
    default <T> T argument(String name) {
        return (T) arguments().get(name);
    }

    static ActionDefinition simple(String name) {
        return create(name, Map.of());
    }

    static ActionDefinition create(String name, Map<String, Object> arguments) {
        return new Default(name, arguments);
    }

    /**
     * Default immutable {@link ActionDefinition} implementation. 🧱
     */
    record Default(String name, Map<String, Object> arguments) implements ActionDefinition {

        public Default {
            notBlank(name, "name");
            arguments = Map.copyOf(new LinkedHashMap<>(nonNull(arguments, "arguments")));
        }

        public Default(String name) {
            this(name, Map.of());
        }
    }

}