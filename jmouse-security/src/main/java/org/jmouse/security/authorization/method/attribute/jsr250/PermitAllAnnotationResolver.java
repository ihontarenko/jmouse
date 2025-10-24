package org.jmouse.security.authorization.method.attribute.jsr250;

import jakarta.annotation.security.PermitAll;
import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.literal.BooleanLiteralNode;
import org.jmouse.security.authorization.method.AnnotationExpressionAttribute;
import org.jmouse.security.authorization.method.ExpressionAttribute;
import org.jmouse.security.access.MethodExpressionHandler;
import org.jmouse.security.access.Phase;

import java.lang.reflect.Method;

/**
 * ✅ Resolves {@link PermitAll} (JSR-250) annotations into a static
 * {@link ExpressionAttribute} allowing all invocations.
 *
 * <p>This resolver integrates with the method authorization pipeline and
 * produces an attribute representing a constant {@code true} expression —
 * meaning <b>access is always granted</b>.</p>
 *
 * <h3>🧩 Example</h3>
 * <pre>{@code
 * import jakarta.annotation.security.PermitAll;
 *
 * public class PublicController {
 *
 *     @PermitAll
 *     public void status() {
 *         // accessible to everyone
 *     }
 * }
 *
 * // During scanning:
 * PermitAllAnnotationResolver resolver = new PermitAllAnnotationResolver();
 * ExpressionAttribute<?> attr = resolver.resolve(permitAll, method, targetClass, handler);
 * }</pre>
 *
 * <h3>⚙️ Behavior</h3>
 * <ul>
 *   <li>Produces a {@link BooleanLiteralNode} set to {@code true}.</li>
 *   <li>Assigns {@link Phase#BEFORE} to apply pre-invocation authorization.</li>
 *   <li>Ignores any expression parsing or dynamic evaluation logic.</li>
 * </ul>
 *
 * <h3>🚦Order</h3>
 * <p>Returns {@code 5} to ensure execution after core authorization resolvers.</p>
 *
 * @author Ivan Hontarenko
 * @since jMouse Security 1.0
 */
public class PermitAllAnnotationResolver extends Jsr250AnnotationResolver<PermitAll> {

    /** 🧱 Constant expression node representing “true”. */
    private static final Expression PERMIT_ALL_EXPRESSION = new BooleanLiteralNode(true);

    /**
     * 📘 Supported annotation type.
     *
     * @return {@link PermitAll}
     */
    @Override
    public Class<PermitAll> annotationType() {
        return PermitAll.class;
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
     * 🧮 Produces an {@link AnnotationExpressionAttribute} that always evaluates to {@code true}.
     *
     * @param permitAll the {@link PermitAll} annotation instance
     * @param method the annotated method
     * @param targetClass the declaring class
     * @param handler the expression handler (unused)
     * @return attribute representing unconditional access permission
     */
    @Override
    public ExpressionAttribute<PermitAll> resolve(
            PermitAll permitAll,
            Method method,
            Class<?> targetClass,
            MethodExpressionHandler<MethodInvocation> handler
    ) {
        return new AnnotationExpressionAttribute<>(Phase.BEFORE, PERMIT_ALL_EXPRESSION, permitAll);
    }

    /**
     * ❌ {@inheritDoc}
     *
     * <p>This annotation does not expose any expression string; method unsupported.</p>
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    protected String getExpressionString(PermitAll annotation) {
        throw new UnsupportedOperationException();
    }
}
