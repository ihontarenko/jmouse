package org.jmouse.beans.conditions;

import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.core.reflection.annotation.MergedAnnotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

/**
 * 📌 Metadata describing conditional context for a bean.
 * <p>
 * Provides access to merged annotations and related elements.
 * </p>
 */
public interface ConditionalMetadata {

    /**
     * 🧱 Definition of the target bean.
     *
     * @return bean definition
     */
    BeanDefinition getBeanDefinition();

    /**
     * 🧬 Resolved annotation tree (with meta-annotations).
     *
     * @return merged annotation
     */
    MergedAnnotation getMergedAnnotation();

    /**
     * 🎯 Get specific annotation instance from merged structure.
     *
     * @param annotationType class of desired annotation
     * @return annotation instance
     * @throws ClassCastException if annotation is missing
     */
    default <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        Optional<MergedAnnotation> annotation = getMergedAnnotation().getMerged(annotationType);

        if (annotation.isPresent()) {
            return (A) annotation.get().getAnnotation();
        }

        throw new ClassCastException(annotation.getClass().getName() + " is not a " + annotationType.getName());
    }

    /**
     * 🧩 Source element where annotation is declared.
     *
     * @return annotated element
     */
    default AnnotatedElement getAnnotatedElement() {
        return getMergedAnnotation().getAnnotatedElement();
    }

}
