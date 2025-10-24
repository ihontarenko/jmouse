package org.jmouse.security.authorization.method.attribute.jsr250;

import jakarta.annotation.security.DenyAll;
import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.literal.BooleanLiteralNode;
import org.jmouse.security.authorization.method.AnnotationExpressionAttribute;
import org.jmouse.security.authorization.method.ExpressionAttribute;
import org.jmouse.security.access.MethodExpressionHandler;
import org.jmouse.security.access.Phase;

import java.lang.reflect.Method;

/**
 * 🚫 Resolves {@link DenyAll} (JSR-250) annotations into
 * a static {@link ExpressionAttribute} denying all invocations.
 *
 * <p>This resolver participates in JSR-250 method authorization and
 * creates an attribute representing a constant {@code false} expression,
 * meaning <b>access is always denied</b>.</p>
 *
 * <h3>🧩 Example</h3>
 * <pre>{@code
 * import jakarta.annotation.security.DenyAll;
 *
 * public class AdminController {
 *
 *     @DenyAll
 *     public void deleteEverything() {
 *         // will never be authorized
 *     }
 * }
 *
 * // During scanning:
 * DenyAllAnnotationResolver resolver = new DenyAllAnnotationResolver();
 * ExpressionAttribute<?> attr = resolver.resolve(denyAll, method, targetClass, handler);
 * }</pre>
 *
 * <h3>⚙️ Behavior</h3>
 * <ul>
 *   <li>Always produces an {@link Expression} equal to a Boolean literal {@code false}.</li>
 *   <li>Sets {@link Phase#BEFORE} to ensure pre-execution denial.</li>
 *   <li>Does not support dynamic expression parsing (throws on attempt).</li>
 * </ul>
 *
 * <h3>🚦Order</h3>
 * <p>Returns {@code 5} so it executes after standard authorization resolvers.</p>
 *
 * @author Ivan Hontarenko
 * @since jMouse Security 1.0
 */
public class DenyAllAnnotationResolver extends Jsr250AnnotationResolver<DenyAll> {

    /**
     * 🧱 Constant expression node representing "false".
     */
    private static final Expression DENY_ALL_EXPRESSION = new BooleanLiteralNode(false);

    /**
     * 📘 Supported annotation type.
     *
     * @return {@link DenyAll}
     */
    @Override
    public Class<DenyAll> annotationType() {
        return DenyAll.class;
    }

    /**
     * ⚙️ Resolver order priority.
     *
     * @return {@code 5}
     */
    @Override
    public int order() {
        return 5;
    }

    /**
     * 🧮 Produces an {@link AnnotationExpressionAttribute} that always evaluates to {@code false}.
     *
     * @param annotation the {@link DenyAll} annotation instance
     * @param method the annotated method
     * @param targetClass the declaring class
     * @param handler the expression handler (unused here)
     * @return attribute representing unconditional access denial
     */
    @Override
    public ExpressionAttribute<DenyAll> resolve(
            DenyAll annotation,
            Method method,
            Class<?> targetClass,
            MethodExpressionHandler<MethodInvocation> handler
    ) {
        return new AnnotationExpressionAttribute<>(Phase.BEFORE, DENY_ALL_EXPRESSION, annotation);
    }

    /**
     * ❌ {@inheritDoc}
     *
     * <p>This annotation does not provide an expression string; method unsupported.</p>
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    protected String getExpressionString(DenyAll annotation) {
        throw new UnsupportedOperationException();
    }
}
