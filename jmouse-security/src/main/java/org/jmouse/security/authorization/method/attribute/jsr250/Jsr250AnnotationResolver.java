package org.jmouse.security.authorization.method.attribute.jsr250;

import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.el.node.Expression;
import org.jmouse.security.authorization.method.AnnotationExpressionAttribute;
import org.jmouse.security.authorization.method.ExpressionAttribute;
import org.jmouse.security.access.MethodExpressionHandler;
import org.jmouse.security.authorization.method.attribute.AttributeResolver;
import org.jmouse.security.access.Phase;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * 🧩 Base class for JSR-250 annotation resolvers (e.g. {@code @RolesAllowed}, {@code @PermitAll}, {@code @DenyAll}).
 *
 * <p>Provides a standard implementation of {@link AttributeResolver} that:
 * <ul>
 *   <li>Obtains an expression string from the concrete resolver via {@link #getExpressionString(Annotation)}.</li>
 *   <li>Compiles it using the supplied {@link MethodExpressionHandler}.</li>
 *   <li>Wraps it into an {@link AnnotationExpressionAttribute} with {@link Phase#BEFORE}.</li>
 * </ul>
 *
 * <h3>🧱 Example implementation</h3>
 * <pre>{@code
 * public class RolesAllowedAnnotationResolver extends Jsr250AnnotationResolver<RolesAllowed> {
 *     @Override
 *     protected String getExpressionString(RolesAllowed annotation) {
 *         // Example: convert roles array into expression
 *         return "principal.hasAnyRole('" + String.join("','", annotation.value()) + "')";
 *     }
 *
 *     @Override
 *     public Class<RolesAllowed> annotationType() {
 *         return RolesAllowed.class;
 *     }
 * }
 * }</pre>
 *
 * <h3>⚙️ Behavior</h3>
 * <ul>
 *   <li>Sets phase to {@link Phase#BEFORE} — JSR-250 checks happen before invocation.</li>
 *   <li>Leaves expression creation strategy to subclasses.</li>
 *   <li>Can be extended by static resolvers (e.g. {@code PermitAll}, {@code DenyAll}) or dynamic ones.</li>
 * </ul>
 *
 * @param <A> the annotation type being resolved
 * @author Ivan Hontarenko
 * @since jMouse Security 1.0
 */
public abstract class Jsr250AnnotationResolver<A extends Annotation> implements AttributeResolver<A> {

    /**
     * 🧮 Compiles an EL expression derived from the annotation into an {@link ExpressionAttribute}.
     *
     * @param annotation the JSR-250 annotation instance
     * @param method the annotated method
     * @param targetClass the declaring class
     * @param handler the expression handler providing EL compiler
     * @return compiled {@link ExpressionAttribute}
     */
    @Override
    public ExpressionAttribute<A> resolve(
            A annotation,
            Method method,
            Class<?> targetClass,
            MethodExpressionHandler<MethodInvocation> handler
    ) {
        Expression expression = handler.getExpressionLanguage().compile(getExpressionString(annotation));
        return new AnnotationExpressionAttribute<>(Phase.BEFORE, expression, annotation);
    }

    /**
     * 🧾 Extracts the raw expression string from the given annotation.
     *
     * <p>Implementations define how an annotation (e.g. {@code @RolesAllowed})
     * translates its attributes into an expression understood by the EL engine.</p>
     *
     * @param annotation the annotation instance
     * @return the expression string to compile
     */
    protected abstract String getExpressionString(A annotation);
}
