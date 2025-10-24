package org.jmouse.security.authorization.method.attribute;

import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.el.node.Expression;
import org.jmouse.el.parser.ParseException;
import org.jmouse.security.authorization.method.AnnotationExpressionAttribute;
import org.jmouse.security.authorization.method.ExpressionAttribute;
import org.jmouse.security.access.MethodExpressionHandler;
import org.jmouse.security.access.annotation.Authorize;

import java.lang.reflect.Method;

/**
 * 🔐 Resolves {@link Authorize} annotations into {@link ExpressionAttribute} instances.
 *
 * <p>Used by the method authorization subsystem to extract and compile the EL expression
 * from {@link Authorize#value()} and wrap it in a {@link AnnotationExpressionAttribute}.</p>
 *
 * <h3>🧩 Example</h3>
 * <pre>{@code
 * @Authorize(value = "principal.role == 'ADMIN'", phase = Phase.BEFORE)
 * public void secureAction() { ... }
 *
 * // During scanning:
 * AuthorizeAnnotationResolver resolver = new AuthorizeAnnotationResolver();
 * ExpressionAttribute<?> attr = resolver.resolve(authorize, method, targetClass, handler);
 * }</pre>
 *
 * <h3>⚙️ Behavior</h3>
 * <ul>
 *   <li>Compiles the {@link Authorize#value()} expression using the provided
 *       {@link MethodExpressionHandler}’s expression language.</li>
 *   <li>Wraps the parsed {@link Expression} together with the phase and source annotation.</li>
 *   <li>Throws {@link IllegalArgumentException} if expression compilation fails.</li>
 * </ul>
 *
 * @author Ivan Hontarenko
 */
public class AuthorizeAnnotationResolver implements AttributeResolver<Authorize> {

    /**
     * 📘 Target annotation type this resolver supports.
     *
     * @return {@link Authorize} annotation type
     */
    @Override
    public Class<Authorize> annotationType() {
        return Authorize.class;
    }

    /**
     * 🧮 Converts the {@link Authorize} annotation into an {@link ExpressionAttribute}.
     *
     * @param authorize the {@link Authorize} annotation instance
     * @param method the annotated method
     * @param targetClass the declaring class
     * @param handler the expression handler providing the EL compiler
     * @return a compiled {@link AnnotationExpressionAttribute}
     * @throws IllegalArgumentException if the expression cannot be parsed
     */
    @Override
    public ExpressionAttribute<Authorize> resolve(
            Authorize authorize,
            Method method,
            Class<?> targetClass,
            MethodExpressionHandler<MethodInvocation> handler
    ) {
        try {
            Expression expression = handler.getExpressionLanguage().compile(authorize.value());
            return new AnnotationExpressionAttribute<>(authorize.phase(), expression, authorize);
        } catch (ParseException e) {
            throw new IllegalArgumentException(
                    "❌ Could not parse @Authorize expression '%s' at %s#%s"
                            .formatted(authorize.value(), targetClass.getSimpleName(), method.getName()),
                    e
            );
        }
    }

    /**
     * ⚙️ Resolver order priority.
     *
     * @return -10 (higher priority than default)
     */
    @Override
    public int order() {
        return -10;
    }
}
