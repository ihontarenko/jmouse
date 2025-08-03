package org.jmouse.core.reflection.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.*;

public final class MergedAnnotations {

    private final Map<Class<? extends Annotation>, List<MergedAnnotation>> typeIndex;

    private MergedAnnotations(List<MergedAnnotation> roots) {
        this.typeIndex = flattenAndIndex(roots);
    }

    public static MergedAnnotations ofAnnotatedElement(AnnotatedElement element) {
        Set<AnnotationData>    scanned = AnnotationScanner.scan(element);
        List<MergedAnnotation> roots   = scanned.stream()
                .map(data -> new MergedAnnotation(data, null)).toList();
        return new MergedAnnotations(roots);
    }

    public Optional<MergedAnnotation> get(Class<? extends Annotation> type) {
        return Optional.ofNullable(typeIndex.get(type))
                .flatMap(list -> list.stream().findFirst());
    }

    public List<MergedAnnotation> getAll(Class<? extends Annotation> type) {
        return typeIndex.getOrDefault(type, List.of());
    }

    public List<MergedAnnotation> all() {
        return typeIndex.values().stream().flatMap(List::stream).toList();
    }

    private Map<Class<? extends Annotation>, List<MergedAnnotation>> flattenAndIndex(List<MergedAnnotation> roots) {
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

