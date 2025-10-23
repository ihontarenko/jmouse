package org.jmouse.security.authorization.method.attribute.jsr250;

import jakarta.annotation.security.RolesAllowed;
import org.jmouse.core.Streamable;
import org.jmouse.util.StringHelper;

/**
 * 🧩 Resolver for the {@link RolesAllowed} JSR-250 annotation.
 * <p>
 * Converts annotation metadata into an evaluable security expression
 * that checks if the current {@code authentication.authorities}
 * contain any of the declared roles.
 * </p>
 *
 * <h3>Example 🔐</h3>
 * <pre>{@code
 * @RolesAllowed({"ADMIN"})
 * public void updateUser(...) { ... }
 * }</pre>
 *
 * Generates the following EL expression:
 * <pre>{@code
 * (authentication.authorities | map(a -> a.authority))
 *     is containsAny("ADMIN")
 * }</pre>
 *
 * <p>
 * The resulting expression is later evaluated by
 * {@code SecurityExpressionEvaluator} inside the authorization pipeline.
 * </p>
 */
public class RolesAllowedAnnotationResolver extends Jsr250AnnotationResolver<RolesAllowed> {

    /**
     * 📘 Returns the handled annotation type — {@link RolesAllowed}.
     */
    @Override
    public Class<RolesAllowed> annotationType() {
        return RolesAllowed.class;
    }

    /**
     * 🧮 Builds a security expression string matching any of the specified roles.
     *
     * @param annotation the {@link RolesAllowed} annotation instance
     * @return an expression string for EL evaluation
     */
    @Override
    protected String getExpressionString(RolesAllowed annotation) {
        String contains = Streamable.of(annotation.value())
                .map(StringHelper::unquote)
                .map(StringHelper::quote)
                .joining(", ");
        return "(authentication.authorities | map(a -> a.authority)) "
                + "is containsAny(" + contains + ")";
    }
}
