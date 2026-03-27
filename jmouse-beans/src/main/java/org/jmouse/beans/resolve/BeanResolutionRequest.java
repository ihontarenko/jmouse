package org.jmouse.beans.resolve;

import org.jmouse.beans.BeanContext;
import org.jmouse.core.reflection.InferredType;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;

import static org.jmouse.beans.resolve.NullableSupport.isNullable;

public interface BeanResolutionRequest {

    static BeanResolutionRequest forDependency(BeanContext context, InferredType beanType, String beanName, AnnotatedElement element, boolean required) {
        return new Default(context, beanType, beanName, element, required);
    }

    static BeanResolutionRequest forType(BeanContext context, InferredType beanType, boolean required) {
        return new Default(context, beanType, null, null, required);
    }

    static BeanResolutionRequest forName(BeanContext context, InferredType beanType, String beanName, boolean required) {
        return new Default(context, beanType, beanName, null, required);
    }

    static BeanResolutionRequest forParameter(BeanContext context, Parameter parameter) {
        return new Default(context, InferredType.forParameter(parameter), null, parameter, !isNullable(parameter));
    }

    static BeanResolutionRequest forField(BeanContext context, Field field, String beanName) {
        return new Default(context, InferredType.forField(field), beanName, field, !isNullable(field));
    }

    record Default(
            BeanContext context,
            InferredType beanType,
            String beanName,
            AnnotatedElement source,
            boolean required
    ) implements BeanResolutionRequest { }

    BeanContext context();

    InferredType beanType();

    String beanName();

    boolean required();

    AnnotatedElement source();

    default Class<?> classType() {
        return beanType().getClassType();
    }

    default <A extends Annotation> A get(Class<A> annotationType) {
        AnnotatedElement annotatedSource = source();
        return annotatedSource != null ? annotatedSource.getAnnotation(annotationType) : null;
    }

    default boolean has(Class<? extends Annotation> annotationType) {
        AnnotatedElement annotatedSource = source();
        return annotatedSource != null && annotatedSource.isAnnotationPresent(annotationType);
    }

}