package org.jmouse.beans.resolve;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.definition.BeanDependency;
import org.jmouse.core.reflection.InferredType;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;

/**
 * Describes a bean resolution request. 🧩
 *
 * <p>
 * {@code BeanResolutionRequest} represents all metadata required to resolve a dependency:
 * </p>
 * <ul>
 *     <li>target {@link BeanContext}</li>
 *     <li>requested dependency type</li>
 *     <li>optional bean name</li>
 *     <li>source element (parameter, field, etc.)</li>
 *     <li>required flag</li>
 * </ul>
 *
 * <p>
 * Implementations correspond to different resolution scenarios such as:
 * </p>
 * <ul>
 *     <li>plain type resolution</li>
 *     <li>named bean resolution</li>
 *     <li>method parameter injection</li>
 *     <li>field injection</li>
 *     <li>explicit dependency descriptors</li>
 * </ul>
 */
public sealed interface BeanResolutionRequest
        permits BeanResolutionRequest.TypeResolutionRequest,
                BeanResolutionRequest.NamedTypeResolutionRequest,
                BeanResolutionRequest.ParameterResolutionRequest,
                BeanResolutionRequest.FieldResolutionRequest,
                BeanResolutionRequest.DependencyResolutionRequest {

    /**
     * Creates a request from a {@link BeanDependency}.
     */
    static BeanResolutionRequest forDependency(BeanContext beanContext, BeanDependency dependency) {
        return new DependencyResolutionRequest(beanContext, dependency.javaType(), dependency.name(), true);
    }

    /**
     * Creates a required type-based request.
     */
    static BeanResolutionRequest forType(BeanContext beanContext, InferredType beanType) {
        return new TypeResolutionRequest(beanContext, beanType, true);
    }

    /**
     * Creates a type-based request with explicit required flag.
     */
    static BeanResolutionRequest forType(BeanContext beanContext, InferredType beanType, boolean required) {
        return new TypeResolutionRequest(beanContext, beanType, required);
    }

    /**
     * Creates a named type-based request.
     */
    static BeanResolutionRequest forName(BeanContext beanContext, InferredType beanType, String beanName, boolean required) {
        return new NamedTypeResolutionRequest(beanContext, beanType, beanName, required);
    }

    /**
     * Creates a parameter-based resolution request.
     */
    static BeanResolutionRequest forParameter(BeanContext beanContext, Parameter parameter) {
        return new ParameterResolutionRequest(beanContext, parameter, !NullableSupport.isNullable(parameter));
    }

    /**
     * Creates a field-based resolution request.
     */
    static BeanResolutionRequest forField(BeanContext beanContext, Field field, String beanName) {
        return new FieldResolutionRequest(beanContext, field, beanName, !NullableSupport.isNullable(field));
    }

    /**
     * Returns the current bean context.
     */
    BeanContext beanContext();

    /**
     * Returns the dependency type to resolve.
     */
    InferredType beanType();

    /**
     * Indicates whether the dependency is required.
     */
    boolean required();

    /**
     * Returns the annotated source element (parameter, field, etc.).
     *
     * @return source element or {@code null}
     */
    AnnotatedElement annotatedElement();

    /**
     * Returns the raw (erased) class of the dependency type.
     */
    default Class<?> classType() {
        return beanType().getClassType();
    }

    /**
     * Returns an annotation from the source element.
     *
     * @param annotationType annotation type
     * @param <A>            annotation type
     * @return annotation or {@code null}
     */
    default <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        AnnotatedElement annotatedSource = annotatedElement();
        return annotatedSource != null ? annotatedSource.getAnnotation(annotationType) : null;
    }

    /**
     * Checks whether the source element has the given annotation.
     *
     * @param annotationType annotation type
     * @return {@code true} if present
     */
    default boolean hasAnnotation(Class<? extends Annotation> annotationType) {
        AnnotatedElement annotatedSource = annotatedElement();
        return annotatedSource != null && annotatedSource.isAnnotationPresent(annotationType);
    }

    /**
     * Returns the bean name (if applicable).
     *
     * @return bean name or {@code null}
     */
    default String beanName() {
        return null;
    }

    /**
     * Resolution request for a plain type.
     */
    record TypeResolutionRequest(BeanContext beanContext, InferredType beanType, boolean required)
            implements BeanResolutionRequest {

        @Override
        public AnnotatedElement annotatedElement() {
            return null;
        }
    }

    /**
     * Resolution request for a named type.
     */
    record NamedTypeResolutionRequest(BeanContext beanContext, InferredType beanType, String beanName, boolean required)
            implements BeanResolutionRequest {

        @Override
        public AnnotatedElement annotatedElement() {
            return null;
        }
    }

    /**
     * Resolution request based on a method or constructor parameter.
     */
    record ParameterResolutionRequest(BeanContext beanContext, Parameter parameter, boolean required)
            implements BeanResolutionRequest {

        @Override
        public InferredType beanType() {
            return InferredType.forParameter(parameter);
        }

        @Override
        public AnnotatedElement annotatedElement() {
            return parameter;
        }
    }

    /**
     * Resolution request based on a field.
     */
    record FieldResolutionRequest(BeanContext beanContext, Field field, String beanName, boolean required)
            implements BeanResolutionRequest {

        @Override
        public InferredType beanType() {
            return InferredType.forField(field);
        }

        @Override
        public AnnotatedElement annotatedElement() {
            return field;
        }
    }

    /**
     * Resolution request based on an explicit dependency descriptor.
     */
    record DependencyResolutionRequest(BeanContext beanContext, InferredType beanType, String beanName, boolean required)
            implements BeanResolutionRequest {

        @Override
        public AnnotatedElement annotatedElement() {
            return null;
        }
    }
}