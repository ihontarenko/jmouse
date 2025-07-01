package org.jmouse.core.reflection.annotation;

import java.lang.annotation.*;
import java.lang.reflect.AnnotatedElement;
import java.util.Set;

/**
 * 🧬 Marker interface for synthetic annotations.
 * <p>
 * Synthetic annotations are not present in the source code but are generated at runtime
 * to simulate annotation behavior (e.g., for internal tagging, fallback defaults, or meta-logic).
 *
 * <p>
 * This interface and its internal {@link Synthetic} annotation can be used to create
 * artificial annotations for {@link AnnotationData} composition and inspection.
 * </p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
@SyntheticAnnotation.Synthetic
public interface SyntheticAnnotation {

    /**
     * 🛠 Creates synthetic {@link AnnotationData} for a given element.
     * <p>
     * This method wraps the target {@link AnnotatedElement} with a synthetic annotation
     * to simulate annotation-based processing where no real annotation is present.
     * </p>
     *
     * @param element annotated element (class, method, etc.)
     * @return synthetic annotation data wrapper
     */
    static AnnotationData forAnnotatedElement(AnnotatedElement element) {
        Set<AnnotationData> annotations = AnnotationScanner.scan(element);
        Annotation syntheticInstance = SyntheticAnnotation.class.getAnnotation(Synthetic.class);
        return new AnnotationData(syntheticInstance, element, null, annotations, -1);
    }

    /**
     * 🧪 Designates the annotation as synthetic.
     * <p>
     * Used for marking internal or auto-generated annotations that do not originate from source code.
     * </p>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Synthetic {
    }

}
