package org.jmouse.core.reflection.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;
import java.util.function.Function;

final public class Annotations {

    private Annotations() {
    }

    public static <A extends Annotation> Function<AnnotatedElement, A> findUniqueAnnotation(Class<A> type) {
        return element -> {
            AnnotationRepository       repository = AnnotationRepository.ofAnnotatedElement(element);
            Optional<MergedAnnotation> optional   = repository.get(type);
            return optional.map(a -> a.getNativeAnnotation(type)).orElse(null);
        };
    }

}
