package org.jmouse.core.reflection.annotation;

import java.lang.annotation.*;
import java.lang.reflect.AnnotatedElement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AnnotationScanner {

    private static final Set<Class<? extends Annotation>> IGNORED = Set.of(Target.class, Retention.class,
                                                                           Documented.class, Inherited.class);

    private static final Map<AnnotatedElement, Set<AnnotationData>> CACHE = new ConcurrentHashMap<>();

    public static Set<AnnotationData> scan(AnnotatedElement element) {
        return CACHE.computeIfAbsent(element, AnnotationScanner::scanInternal);
    }

    public static Set<AnnotationData> scanInternal(AnnotatedElement element) {
        Set<AnnotationData>   annotations = new LinkedHashSet<>();
        Set<String>           visited     = new HashSet<>();
        Deque<AnnotationData> stack       = new ArrayDeque<>();

        for (Annotation annotation : element.getAnnotations()) {
            stack.push(new AnnotationData(annotation, element, null, 0));
        }

        while (!stack.isEmpty()) {
            AnnotationData              item = stack.pop();
            Class<? extends Annotation> type = item.annotationType();
            String                      key  = item.annotatedElement().toString() + "#" + type.getName();

            if (visited.contains(key) || isIgnored(type)) {
                continue;
            }

            AnnotationData data = new AnnotationData(item.annotation(), item.annotatedElement(), item.parent(), 0);

            annotations.add(data);

            visited.add(key);

            for (Annotation annotation : type.getAnnotations()) {
                stack.push(new AnnotationData(annotation, type, data, item.depth() + 1));
            }
        }

        return annotations;
    }

    private static boolean isIgnored(Class<? extends Annotation> type) {
        return type.getName().startsWith("java.lang.annotation")
                || IGNORED.contains(type);
    }

}
