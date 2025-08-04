package org.jmouse.core.reflection.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.*;

/**
 * 🧩 Aggregates and flattens annotations from an {@link AnnotatedElement}, including meta-annotations.
 *
 * <p>This class indexes all discovered annotations by type,
 * allowing access to merged and synthesized annotation models.</p>
 *
 * <p>Meta-annotations (e.g., {@code @Component} on a {@code @Controller}) are also included recursively.</p>
 *
 * <pre>{@code
 * @Controller
 * public class MyController {}
 *
 * AnnotationRepository annotations = AnnotationRepository.ofAnnotatedElement(GetMapping.class);
 * Optional<MergedAnnotation> controller = annotations.get(Controller.class);
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class AnnotationRepository {

    private final Map<Class<? extends Annotation>, List<MergedAnnotation>> index;

    private AnnotationRepository(List<MergedAnnotation> roots) {
        this.index = indexing(roots);
    }

    /**
     * Creates merged annotations from the specified element,
     * including its direct annotations and recursively their meta-annotations.
     *
     * @param element the annotated element to scan
     * @return a {@link AnnotationRepository} instance
     */
    public static AnnotationRepository ofAnnotatedElement(AnnotatedElement element) {
        Set<AnnotationData>    scanned = AnnotationScanner.scan(element);
        List<MergedAnnotation> roots   = scanned.stream().map(data
                -> new MergedAnnotation(data, null)).toList();

        return new AnnotationRepository(roots);
    }

    /**
     * Returns the first merged annotation of the given type, if present.
     *
     * @param type the annotation class
     * @return an optional merged annotation
     */
    public Optional<MergedAnnotation> get(Class<? extends Annotation> type) {
        return Optional.ofNullable(index.get(type))
                .flatMap(list -> list.stream().findFirst());
    }

    /**
     * Returns all merged annotations of the given type, including any meta-annotations.
     *
     * @param type the annotation class
     * @return list of merged annotations (possibly empty)
     */
    public List<MergedAnnotation> getAll(Class<? extends Annotation> type) {
        return index.getOrDefault(type, List.of());
    }

    /**
     * Returns all merged annotations of all types found on the element.
     *
     * @return flattened list of all annotations including meta-annotations
     */
    public List<MergedAnnotation> all() {
        return index.values().stream().flatMap(List::stream).toList();
    }

    /**
     * Flattens the tree of root annotations and their meta-annotations into a map indexed by type.
     */
    private Map<Class<? extends Annotation>, List<MergedAnnotation>> indexing(List<MergedAnnotation> roots) {
        Map<Class<? extends Annotation>, List<MergedAnnotation>> index = new LinkedHashMap<>();
        Deque<MergedAnnotation>                                  queue = new ArrayDeque<>(roots);

        while (!queue.isEmpty()) {
            MergedAnnotation current = queue.poll();
            index.computeIfAbsent(current.getAnnotationType(), __
                    -> new ArrayList<>()).add(current);
            queue.addAll(current.getMetas());
        }

        return index;
    }
}
