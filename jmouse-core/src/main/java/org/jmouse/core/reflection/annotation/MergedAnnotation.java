package org.jmouse.core.reflection.annotation;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.util.Streamable;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * 🧬 Represents a merged annotation with its meta-annotations.
 *
 * Preserves the full hierarchy of annotations (including meta-levels)
 * and allows flat traversal or filtering by type.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
public class MergedAnnotation {

    private final AnnotationData         annotationData;
    private final Set<MergedAnnotation>  metaAnnotations = new LinkedHashSet<>();
    private       List<MergedAnnotation> flatAnnotations;

    /**
     * 🔁 Build merged annotation tree from base data.
     */
    public MergedAnnotation(AnnotationData annotationData) {
        this.annotationData = annotationData;

        for (AnnotationData meta : annotationData.metas()) {
            metaAnnotations.add(new MergedAnnotation(meta));
        }
    }

    /**
     * 🎯 The actual annotation instance.
     */
    public Annotation getAnnotation() {
        return annotationData.annotation();
    }

    /**
     * 📘 Type of this annotation.
     */
    public Class<? extends Annotation> getAnnotationType() {
        return annotationData.annotationType();
    }

    /**
     * 📎 Optional parent annotation in the meta-chain.
     */
    public Optional<MergedAnnotation> getParent() {
        return annotationData.getParent().map(MergedAnnotation::new);
    }

    /**
     * 🧭 Depth in the annotation hierarchy (0 = direct).
     */
    public int getDepth() {
        return annotationData.depth();
    }

    /**
     * 🌐 Direct meta-annotations of this annotation.
     */
    public Set<MergedAnnotation> getMetas() {
        return metaAnnotations;
    }

    /**
     * 🔍 Search for merged annotation by type.
     *
     * @param type annotation class to search for
     * @return optional merged annotation
     */
    public <A extends Annotation> Optional<MergedAnnotation> getMerged(Class<A> type) {
        Matcher<Annotation> matcher = AnnotationMatcher.isAnnotation(type);
        return Streamable.findFirst(getFlattened(), ma -> matcher.matches(ma.getAnnotation()));
    }

    /**
     * 🧷 Whether this annotation is a meta-annotation (depth > 0).
     */
    public boolean isMetaAnnotation() {
        return getDepth() > 0;
    }

    /**
     * 🔍 Check if a merged annotation of the given type exists.
     */
    public boolean isAnnotationPresent(Class<? extends Annotation> type) {
        return getMerged(type).isPresent();
    }

    /**
     * 🔍 Check presence by instance (delegates to type-based check).
     */
    public boolean isAnnotationPresent(Annotation annotation) {
        return isAnnotationPresent(annotation.annotationType());
    }

    /**
     * 📚 Flatten hierarchy to list (breadth-first).
     */
    public List<MergedAnnotation> getFlattened() {
        List<MergedAnnotation> flattened = this.flatAnnotations;

        if (flattened == null) {
            Deque<MergedAnnotation> stack = new ArrayDeque<>();

            flattened = new ArrayList<>();
            stack.push(this);

            while (!stack.isEmpty()) {
                MergedAnnotation current = stack.pop();
                flattened.add(current);
                stack.addAll(current.getMetas());
            }

            this.flatAnnotations = flattened;
        }

        return flattened;
    }

    @Override
    public String toString() {
        return "@%s (depth=%d) on %s".formatted(
                getAnnotationType().getSimpleName(),
                getDepth(),
                annotationData.annotatedElement()
        );
    }
}
