package org.jmouse.core.reflection.annotation;

import java.lang.annotation.*;
import java.lang.reflect.AnnotatedElement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🔍 Scans annotations recursively, including meta-annotations.
 * <p>Results are cached per element.</p>
 */
public class AnnotationScanner {

    private static final Set<Class<? extends Annotation>> IGNORED = Set.of(
            Target.class, Retention.class, Documented.class, Inherited.class
    );

    private static final Map<AnnotatedElement, Set<AnnotationData>> CACHE = new ConcurrentHashMap<>();

    /**
     * 🧠 Get scanned annotations from cache or perform scan.
     */
    public static Set<AnnotationData> scan(AnnotatedElement element) {
        return CACHE.computeIfAbsent(element, e -> {
            Set<AnnotationData> annotations = new LinkedHashSet<>();
            scanInternal(e, annotations);
            return annotations;
        });
    }

    public static void scanInternal(AnnotatedElement element, Set<AnnotationData> annotations) {
        scanInternal(element, annotations, null, 0, new HashSet<>());
    }

    private static void scanInternal(
            AnnotatedElement element, Set<AnnotationData> annotations, AnnotationData parent,
            int depth, Set<String> visited) {

        for (Annotation annotation : element.getAnnotations()) {
            Class<? extends Annotation> type = annotation.annotationType();
            String                      key  = element + "#" + type.getName();

            if (visited.contains(key) || isIgnorable(type)) {
                continue;
            }

            AnnotationData current = new AnnotationData(annotation, element, parent, depth);

            annotations.add(current);
            visited.add(key);

            scanInternal(type, current.metas(), current, depth + 1, visited);
        }
    }

    /**
     * 🚫 Ignore standard meta-annotations.
     */
    public static boolean isIgnorable(Class<? extends Annotation> type) {
        return type.getName().startsWith("java.lang.annotation")
                || IGNORED.contains(type);
    }

}
