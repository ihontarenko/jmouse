package org.jmouse.beans.resolve;

import org.jmouse.beans.BeanContext;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.core.reflection.annotation.AnnotationRepository;
import org.jmouse.core.reflection.annotation.MergedAnnotation;

import java.lang.annotation.Annotation;
import java.util.Optional;

/**
 * Describes a bean resolution request. 🧩
 *
 * <p>Encapsulates target type, optional bean name, source element, and required flag.</p>
 */
public interface BeanResolutionRequest {

    /**
     * Creates request for dependency-based resolution.
     *
     * @param context       bean context
     * @param beanType      requested bean type
     * @param beanName      optional bean name
     * @param repository    source annotated element
     * @param required      whether dependency is required
     * @return resolution request
     */
    static BeanResolutionRequest forDependency(BeanContext context, InferredType beanType, String beanName, AnnotationRepository repository, boolean required) {
        return new Default(context, beanType, beanName, repository, required);
    }

    /**
     * Creates request for plain type-based resolution.
     *
     * @param context   bean context
     * @param beanType  requested bean type
     * @param required  whether dependency is required
     * @return resolution request
     */
    static BeanResolutionRequest forDependency(BeanContext context, InferredType beanType, boolean required) {
        return new Default(context, beanType, null, null, required);
    }

    /**
     * Default immutable request implementation.
     *
     * @param context   bean context
     * @param beanType  requested bean type
     * @param beanName  optional bean name
     * @param repository    source annotated element
     * @param required  whether dependency is required
     */
    record Default(
            BeanContext context,
            InferredType beanType,
            String beanName,
            AnnotationRepository repository,
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
    AnnotationRepository repository();

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
        Optional<MergedAnnotation> annotation = repository().get(annotationType);
        return annotation.map(a -> a.getNativeAnnotation(annotationType)).orElse(null);

    }

    /**
     * Checks whether source element has given annotation.
     *
     * @param annotationType annotation type
     * @return {@code true} if present
     */
    default boolean has(Class<? extends Annotation> annotationType) {
        return get(annotationType) != null;
    }

}