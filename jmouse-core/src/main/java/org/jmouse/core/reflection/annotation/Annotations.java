package org.jmouse.core.reflection.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Map;
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

    /**
     * 🧩 Builds a function that returns a <em>resolved</em> attribute map for a specific annotation type.
     *
     * <p>The returned function scans an {@link AnnotatedElement} via {@link AnnotationRepository}
     * and, if the annotation is present (directly or via meta-annotations), produces a flat map of
     * attribute {@code name → value} with <strong>all meta-levels merged</strong>
     * (equivalent to {@link MergedAnnotation#asResolvedAll()}). If the annotation is absent,
     * returns {@link java.util.Map#of() an empty map}.</p>
     *
     * <h4>Why “resolved all”?</h4>
     * <p>It gives you a convenient, human-friendly view where the visible annotation’s attributes are
     * combined with attributes inherited from its meta-annotations (breadth-first), using
     * {@link CollisionPolicy#KEEP_EXISTING} so the visible annotation wins on conflicts.</p>
     *
     * <h4>Usage</h4>
     * <pre>{@code
     * // Given:
     * // @PreAuthorize(value = "33 % 3 == 0", index = 666)
     * // @Authorize(phase = Phase.BEFORE) // meta on @PreAuthorize
     *
     * Function<AnnotatedElement, Map<String,Object>> preAttrs = Annotations.attributes(PreAuthorize.class);
     * Map<String,Object> m = preAttrs.apply(MyController.class);
     * // => { value: "33 % 3 == 0", index: 666, phase: BEFORE }
     *
     * // If the element doesn't carry @PreAuthorize (directly or via metas):
     * Map<String,Object> empty = preAttrs.apply(OtherClass.class); // empty map
     * }</pre>
     *
     * @param type the annotation type to resolve (must not be {@code null})
     * @param <A>  annotation generic
     * @return a function mapping {@link AnnotatedElement} → merged attribute map (possibly empty)
     */
    public static <A extends Annotation> Function<AnnotatedElement, Map<String, Object>> attributes(Class<A> type) {
        return e -> {
            AnnotationRepository       repository = AnnotationRepository.ofAnnotatedElement(e);
            Optional<MergedAnnotation> optional   = repository.get(type);
            return optional.map(MergedAnnotation::asResolvedAll).orElseGet(Map::of);
        };
    }

    /**
     * 🎯 Retrieves a single resolved attribute value from an annotation on a given element.
     *
     * <p>This is a convenience wrapper around {@link #attributes(Class)} that extracts
     * one specific attribute by name. The lookup traverses meta-annotations as well,
     * since it internally delegates to {@link MergedAnnotation#asResolvedAll()}.</p>
     *
     * <h4>Usage</h4>
     * <pre>{@code
     * // Example: obtain single attribute value from @PreAuthorize
     * Optional<Phase> optional = Annotations.attribute(
     *      "phase", UserController.class, PreAuthorize.class);
     *
     * // Can be unwrapped or cast:
     * Phase phase = optional.orElse(Phase.DEFAULT);
     * }</pre>
     *
     * <p>If the annotation is not present or the attribute is missing, the result is
     * {@link Optional#empty()}.</p>
     *
     * @param attribute the attribute name to retrieve
     * @param element   the annotated element (e.g., class or method)
     * @param type      the annotation type to inspect
     * @param <A>       annotation generic
     * @return an {@link Optional} containing the resolved attribute value, or empty if not found
     */
    @SuppressWarnings("unchecked")
    public static <A extends Annotation, T> Optional<T> attribute(
            String attribute, AnnotatedElement element, Class<A> type) {
        return Optional.ofNullable((T)attributes(type).apply(element).get(attribute));
    }

}
