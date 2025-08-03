package org.jmouse.core.reflection.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

/**
 * 🔧 Synthesizes annotations with mapped attributes from meta-annotations.
 */
public interface AnnotationSynthesizer {

    <A extends Annotation> A synthesize(Class<A> targetAnnotation, MergedAnnotation annotation);

}
