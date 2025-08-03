package org.jmouse.core.reflection.annotation;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

public class DefaultAnnotationSynthesizer implements AnnotationSynthesizer {


    @Override
    public <A extends Annotation> A synthesize(Class<A> targetAnnotation, MergedAnnotation annotation) {
        Map<String, Object> attributes = new HashMap<>();

//        for (Annotation annotation : sourceAnnotations) {
//            attributes.putAll(mapping.resolveMappings(annotation));
//        }

        return null;// AnnotationInvocationHandler.createProxy();
    }



}
