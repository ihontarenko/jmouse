package org.jmouse.access.spring.policy;

import org.jmouse.access.policy.PlaceholderResolver;
import org.jmouse.access.policy.PolicyException;
import org.springframework.core.env.Environment;

/**
 * {@code ${…}} in a policy file, filled from ordinary application configuration.
 *
 * <p>What it is for: a {@code bootstrap.jmp} shipped inside the artefact can name the founding account
 * without a customer's identifier being compiled into it.
 *
 * <pre>
 * subject ${innoventa.bootstrap.owner} {
 *     grants INSTALLATION_OWNER {@literal @}INSTALLATION
 * }
 * </pre>
 *
 * <p>⚠️ <strong>An unset property is a failure, never an empty string.</strong> A subject identifier
 * of {@code ""} is a grant belonging to an account that cannot exist — harmless right up to the day
 * somebody's identifier is empty for an unrelated reason.
 */
public class EnvironmentPlaceholders implements PlaceholderResolver {

    private final Environment environment;

    public EnvironmentPlaceholders(Environment environment) {
        this.environment = environment;
    }

    @Override
    public String resolve(String key) {
        String value = environment.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new PolicyException(
                    "A policy file writes ${" + key + "}, and no property of that name is set. It is "
                    + "resolved once, at load, from application configuration — left unfilled it would "
                    + "be stored literally and grant to nobody.");
        }

        return value;
    }
}
