package org.jmouse.core.reflection.annotation;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.util.Streamable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🔍 Utility for recursive annotation scanning.
 *
 * Traverses annotations and meta-annotations on a given {@link AnnotatedElement},
 * caching results to improve performance. Useful for detecting composed annotations.
 *
 * Example usage:
 * <pre>{@code
 * boolean present = AnnotationFinder.findAll(Matcher.constant(true), UserController.class).size() > 0;
 * }</pre>
 *
 * ⚠️ Uses a static in-memory cache for performance. Thread-safe for reads.
 * Callers should not mutate returned collections.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
public interface AnnotationFinder {

    /**
     * 🗃️ In-memory cache for already-resolved annotation graphs.
     */
    Map<AnnotatedElement, Collection<Annotation>> CACHE = new ConcurrentHashMap<>();

    /**
     * 🔁 Recursively find all annotations matching a predicate.
     *
     * @param matcher condition to test each annotation
     * @param element annotated element (e.g. class, method, field)
     * @return set of annotations that match the matcher
     */
    static Collection<Annotation> findAll(Matcher<Annotation> matcher, AnnotatedElement element) {
        Collection<Annotation> annotations = CACHE.get(element);

        if (annotations == null) {
            annotations = new HashSet<>();

            Deque<Annotation> stack   = new ArrayDeque<>(Arrays.asList(element.getAnnotations()));
            Set<Annotation>   visited = new HashSet<>();

            while (!stack.isEmpty()) {
                Annotation                  current = stack.pop();
                Class<? extends Annotation> type    = current.annotationType();

                if (visited.contains(current)) {
                    continue;
                }

                annotations.add(current);
                visited.add(current);

                for (Annotation annotation : type.getAnnotations()) {
                    stack.push(annotation);
                }
            }

            CACHE.put(element, annotations);
        }

        return Streamable.of(annotations).filter(matcher::matches).toSet();
    }
}
