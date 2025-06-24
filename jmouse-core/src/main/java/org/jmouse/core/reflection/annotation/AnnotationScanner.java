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
        return CACHE.computeIfAbsent(element, AnnotationScanner::scanInternal);
    }

    /**
     * 🔁 Recursive annotation discovery with depth/meta tracking.
     */
    public static Set<AnnotationData> scanInternal(AnnotatedElement element) {
        Set<AnnotationData>   annotations = new LinkedHashSet<>();
        Set<String>           visited     = new HashSet<>();
        Deque<AnnotationData> stack       = new ArrayDeque<>();
        AnnotationData        parent      = null;

        for (Annotation annotation : element.getAnnotations()) {
            stack.push(new AnnotationData(annotation, element, parent, 0));
        }

        while (!stack.isEmpty()) {
            AnnotationData              item = stack.pop();
            Class<? extends Annotation> type = item.annotationType();
            String                      key  = item.annotatedElement() + "#" + type.getName();

            if (visited.contains(key) || isIgnorable(type)) {
                continue;
            }

            AnnotationData data = new AnnotationData(item.annotation(), item.annotatedElement(), item.parent(), 0);

            annotations.add(data);
            visited.add(key);

            for (Annotation metaAnnotation : type.getAnnotations()) {
                Class<? extends Annotation> metaType = metaAnnotation.annotationType();
                String metaKey = type + "#" + metaType.getName();

                if (visited.contains(metaKey) || isIgnorable(metaType)) {
                    continue;
                }

                AnnotationData nested = new AnnotationData(metaAnnotation, type, data, item.depth() + 1);
                data.metas().add(nested);
                stack.push(nested);
            }
        }

        return annotations;
    }

    /**
     * 🚫 Ignore standard meta-annotations.
     */
    public static boolean isIgnorable(Class<? extends Annotation> type) {
        return type.getName().startsWith("java.lang.annotation")
                || IGNORED.contains(type);
    }

}
