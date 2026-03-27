package org.jmouse.beans.resolve;

import org.jmouse.beans.BeanContext;
import org.jmouse.core.reflection.InferredType;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;

import static org.jmouse.beans.resolve.NullableSupport.isNullable;

/**
 * Describes a bean resolution request. 🧩
 *
 * <p>Encapsulates target type, optional bean name, source element, and required flag.</p>
 */
public interface BeanResolutionRequest {

    /**
     * Creates request for dependency-based resolution.
     *
     * @param context   bean context
     * @param beanType  requested bean type
     * @param beanName  optional bean name
     * @param element   source annotated element
     * @param required  whether dependency is required
     * @return resolution request
     */
    static BeanResolutionRequest forDependency(BeanContext context, InferredType beanType, String beanName, AnnotatedElement element, boolean required) {
        return new Default(context, beanType, beanName, element, required);
    }

    /**
     * Creates request for plain type-based resolution.
     *
     * @param context   bean context
     * @param beanType  requested bean type
     * @param required  whether dependency is required
     * @return resolution request
     */
    static BeanResolutionRequest forType(BeanContext context, InferredType beanType, boolean required) {
        return new Default(context, beanType, null, null, required);
    }

    /**
     * Creates request for name-based resolution.
     *
     * @param context   bean context
     * @param beanType  requested bean type
     * @param beanName  bean name
     * @param required  whether dependency is required
     * @return resolution request
     */
    static BeanResolutionRequest forName(BeanContext context, InferredType beanType, String beanName, boolean required) {
        return new Default(context, beanType, beanName, null, required);
    }

    /**
     * Creates request for parameter injection.
     *
     * @param context    bean context
     * @param parameter  source parameter
     * @return resolution request
     */
    static BeanResolutionRequest forParameter(BeanContext context, Parameter parameter) {
        return new Default(context, InferredType.forParameter(parameter), null, parameter, !isNullable(parameter));
    }

    /**
     * Creates request for field injection.
     *
     * @param context   bean context
     * @param field     source field
     * @param beanName  optional bean name
     * @return resolution request
     */
    static BeanResolutionRequest forField(BeanContext context, Field field, String beanName) {
        return new Default(context, InferredType.forField(field), beanName, field, !isNullable(field));
    }

    /**
     * Default immutable request implementation.
     *
     * @param context   bean context
     * @param beanType  requested bean type
     * @param beanName  optional bean name
     * @param source    source annotated element
     * @param required  whether dependency is required
     */
    record Default(
            BeanContext context,
            InferredType beanType,
            String beanName,
            AnnotatedElement source,
            boolean required
    ) implements BeanResolutionRequest { }

    /**
     * Returns bean context.
     */
    BeanContext context();

    /**
     * Returns requested bean type.
     */
    InferredType beanType();

    /**
     * Returns optional bean name.
     */
    String beanName();

    /**
     * Returns whether dependency is required.
     */
    boolean required();

    /**
     * Returns source annotated element.
     */
    AnnotatedElement source();

    /**
     * Returns raw requested class type.
     *
     * @return raw class type
     */
    default Class<?> classType() {
        return beanType().getClassType();
    }

    /**
     * Returns annotation from source element.
     *
     * @param annotationType annotation type
     * @param <A>            annotation generic type
     * @return annotation instance or {@code null}
     */
    default <A extends Annotation> A get(Class<A> annotationType) {
        return source() != null ? source().getAnnotation(annotationType) : null;
    }

    /**
     * Checks whether source element has given annotation.
     *
     * @param annotationType annotation type
     * @return {@code true} if present
     */
    default boolean has(Class<? extends Annotation> annotationType) {
        return source() != null && source().isAnnotationPresent(annotationType);
    }

}