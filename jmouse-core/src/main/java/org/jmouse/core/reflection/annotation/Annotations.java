package org.jmouse.core.reflection.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;
import java.util.function.Function;

/**
 * 🧩 Utilities for annotation lookup and merging.
 *
 * <p>Thin facade over {@code AnnotationRepository} that returns either native annotations or {@code Optional}s.
 * Stateless and thread-safe.</p>
 */
public final class Annotations {

    private Annotations() { }

    /**
     * 🔎 Builds a finder function for a specific annotation type.
     *
     * <p>The returned function inspects an {@link AnnotatedElement} using {@link AnnotationRepository}
     * and returns the single merged/native annotation instance if present; otherwise returns {@code null}.</p>
     *
     * <h4>Usage</h4>
     * <pre>{@code
     * Function<AnnotatedElement, Authorize> findAuthorize = Annotations.lookup(Authorize.class);
     * Authorize a = findAuthorize.apply(MyController.class);
     * }</pre>
     *
     * @param type the annotation type to look up (must not be {@code null})
     * @param <A>  annotation generic
     * @return a function mapping {@link AnnotatedElement} → {@code A} or {@code null} if not present
     */
    public static <A extends Annotation> Function<AnnotatedElement, A> lookup(Class<A> type) {
        return element -> {
            AnnotationRepository       repository = AnnotationRepository.ofAnnotatedElement(element);
            Optional<MergedAnnotation> optional   = repository.get(type);
            return optional.map(a -> a.getNativeAnnotation(type)).orElse(null);
        };
    }

    /**
     * 🧭 Compatibility alias for code that expects a "unique" finder (returns {@code null} if absent).
     *
     * @see #lookup(Class)
     */
    public static <A extends Annotation> Function<AnnotatedElement, A> findUniqueAnnotation(Class<A> type) {
        return lookup(type);
    }

    /**
     * ⚡ Direct, one-shot lookup that returns {@code null} if the annotation is absent.
     *
     * @param element annotated element (must not be {@code null})
     * @param type    annotation type (must not be {@code null})
     */
    public static <A extends Annotation> A find(AnnotatedElement element, Class<A> type) {
        return lookup(type).apply(element);
    }

    /**
     * ✅ Optional-based variant to avoid {@code null}.
     *
     * @param element annotated element (must not be {@code null})
     * @param type    annotation type (must not be {@code null})
     */
    public static <A extends Annotation> Optional<A> findOptional(AnnotatedElement element, Class<A> type) {
        return Optional.ofNullable(find(element, type));
    }

    /**
     * ✅ Optional-producing finder function (AnnotatedElement → Optional&lt;A&gt;).
     *
     * <p>Convenient when mapping streams of elements.</p>
     */
    public static <A extends Annotation> Function<AnnotatedElement, Optional<A>> lookupOptional(Class<A> type) {
        return element -> Optional.ofNullable(lookup(type).apply(element));
    }
}
