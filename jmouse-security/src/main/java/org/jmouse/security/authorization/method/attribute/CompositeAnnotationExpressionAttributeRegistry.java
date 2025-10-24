package org.jmouse.security.authorization.method.attribute;

import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.core.reflection.annotation.Annotations;
import org.jmouse.security.authorization.method.ExpressionAttribute;
import org.jmouse.security.access.MethodExpressionHandler;
import org.jmouse.security.authorization.method.attribute.jsr250.DenyAllAnnotationResolver;
import org.jmouse.security.authorization.method.attribute.jsr250.PermitAllAnnotationResolver;
import org.jmouse.security.authorization.method.attribute.jsr250.RolesAllowedAnnotationResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * 🧮 Composite registry that discovers *any* supported annotation and converts it into an attribute.
 *
 * <p>Resolution rules:</p>
 * <ol>
 *   <li>Method-level annotations beat class-level annotations.</li>
 *   <li>Among resolvers, lower {@link AttributeResolver#order()} wins.</li>
 *   <li>First matching annotation short-circuits.</li>
 * </ol>
 */
public class CompositeAnnotationExpressionAttributeRegistry
        extends AbstractExpressionAttributeRegistry<ExpressionAttribute<?>> {

    private final List<AttributeResolver<? extends Annotation>> resolvers;

    public CompositeAnnotationExpressionAttributeRegistry(
            MethodExpressionHandler<MethodInvocation> expressionHandler,
            List<AttributeResolver<? extends Annotation>> resolvers
    ) {
        super(expressionHandler);
        this.resolvers = resolvers.stream().sorted(Comparator.comparingInt(AttributeResolver::order)).toList();
    }

    public static ExpressionAttributeRegistry<ExpressionAttribute<?>> defaultRegistry(
            MethodExpressionHandler<MethodInvocation> expressionHandler
    ) {
        return new CompositeAnnotationExpressionAttributeRegistry(
                expressionHandler,
                List.of(
                        new AuthorizeAnnotationResolver(),
                        new RolesAllowedAnnotationResolver(),
                        new PermitAllAnnotationResolver(),
                        new DenyAllAnnotationResolver()
                )
        );
    }

    @Override
    public ExpressionAttribute<?> resolveAttribute(Method method, Class<?> targetClass) {
        ExpressionAttribute<?> methodAttribute = tryResolveOn(method, method, targetClass);

        if (methodAttribute != null) {
            return methodAttribute;
        }

        Class<?> type = getClass(method, targetClass);

        return tryResolveOn(type, method, type);
    }

    private ExpressionAttribute<?> tryResolveOn(AnnotatedElement element, Method method, Class<?> targetClass) {
        for (AttributeResolver<? extends Annotation> resolver : resolvers) {
            @SuppressWarnings("unchecked")
            Class<Annotation>                      type       = (Class<Annotation>) resolver.annotationType();
            Function<AnnotatedElement, Annotation> lookup     = Annotations.lookup(type);
            Annotation                             annotation = lookup.apply(element);

            if (annotation != null) {
                @SuppressWarnings("unchecked")
                AttributeResolver<Annotation> attributeResolver = (AttributeResolver<Annotation>) resolver;
                return attributeResolver.resolve(annotation, method, targetClass, getExpressionHandler());
            }
        }
        return null;
    }
}
