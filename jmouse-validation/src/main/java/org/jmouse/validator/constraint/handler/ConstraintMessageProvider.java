package org.jmouse.validator.constraint.handler;

import org.jmouse.validator.constraint.api.Constraint;

/**
 * Strategy interface responsible for resolving validation messages. 📨
 *
 * <p>
 * {@code ConstraintMessageProvider} centralizes message resolution logic
 * for {@link Constraint} instances. It supports:
 * </p>
 *
 * <ul>
 *     <li>default message resolution from the constraint itself,</li>
 *     <li>overridden (DSL-defined) messages,</li>
 *     <li>external message sources (e.g. i18n bundles, message catalogs).</li>
 * </ul>
 *
 * <h3>Resolution flow</h3>
 *
 * <pre>{@code
 * String message = provider.provideMessage(constraint, overridden);
 * }</pre>
 *
 * <p>
 * If {@code overriddenMessage} is not {@code null}, it takes precedence.
 * Otherwise {@link #provideMessage(Constraint)} is used.
 * </p>
 *
 * <h3>Typical implementations</h3>
 * <ul>
 *     <li>Simple provider returning {@code constraint.message()}</li>
 *     <li>MessageSource-backed provider using {@code constraint.code()}</li>
 *     <li>Localization-aware provider (per request locale)</li>
 * </ul>
 */
public interface ConstraintMessageProvider {

    /**
     * Resolves message for a constraint considering possible override.
     *
     * @param constraint        constraint instance
     * @param overriddenMessage explicit override (may be {@code null})
     * @return resolved message (may be {@code null})
     */
    default String provideMessage(Constraint constraint, String overriddenMessage) {
        return overriddenMessage == null
                ? provideMessage(constraint)
                : overriddenMessage;
    }

    /**
     * Resolves default message for a constraint.
     *
     * @param constraint constraint instance
     * @return resolved message (may be {@code null})
     */
    String provideMessage(Constraint constraint);
}