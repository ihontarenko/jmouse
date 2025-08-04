package org.jmouse.core.reflection.annotation;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.util.Streamable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.*;

/**
 * 🧬 Represents a merged annotation with its meta-annotations.
 * <p>
 * Preserves the full hierarchy of annotations (including meta-levels)
 * and allows flat traversal or filtering by type.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
public class MergedAnnotation {

    private final    MergedAnnotation           parent;
    private final    MergedAnnotation           root;
    private final    AnnotationAttributeMapping annotationMapping;
    private final    AnnotationData             annotationData;
    private final    Set<MergedAnnotation>      metaAnnotations = new LinkedHashSet<>();
    private volatile List<MergedAnnotation>     flatAnnotations;

    public MergedAnnotation(AnnotationData annotationData) {
        this(annotationData, null);
    }

    /**
     * 🔁 Build merged annotation tree from base data.
     */
    public MergedAnnotation(AnnotationData annotationData, MergedAnnotation parent) {
        this.parent = parent;
        this.root = parent == null ? this : parent.getRoot();
        this.annotationData = annotationData;

        for (AnnotationData meta : annotationData.metas()) {
            this.metaAnnotations.add(new MergedAnnotation(meta, this));
        }

        this.annotationMapping = new AnnotationMapping(annotationData.annotation(), getRoot());
    }

    /**
     * 🧩 Wraps all annotations of the given element into a single {@link MergedAnnotation}.
     * <p>
     * Useful as a shortcut to avoid manual scanning and conversion of {@link AnnotationData}
     * into multiple {@code MergedAnnotation} instances. The result acts as a synthetic,
     * aggregate wrapper that allows unified access to annotation metadata.
     * </p>
     *
     * <pre>{@code
     * // Instead of:
     * Set<AnnotationData> annotations = AnnotationScanner.scan(SomeClass.class);
     * List<MergedAnnotation> merged = Streamable.of(annotations).map(MergedAnnotation::new).toList();
     *
     * // Use:
     * MergedAnnotation merged = MergedAnnotation.forElement(SomeClass.class);
     * }</pre>
     *
     * @param element the annotated element to wrap (e.g., class or method)
     * @return a synthetic merged annotation representing all annotations on the element
     */
    public static MergedAnnotation wrapWithSynthetic(AnnotatedElement element) {
        return new MergedAnnotation(SyntheticAnnotation.forAnnotatedElement(element));
    }

    /**
     * 📦 Raw metadata container.
     *
     * @return underlying annotation data
     */
    public AnnotationData getAnnotationData() {
        return annotationData;
    }

    /**
     * 🧩 Element annotated by this annotation (e.g. class, method).
     *
     * @return annotated element
     */
    public AnnotatedElement getAnnotatedElement() {
        return annotationData.annotatedElement();
    }

    /**
     * 🎯 The actual annotation instance.
     */
    public Annotation getNativeAnnotation() {
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
    public Optional<MergedAnnotation> getMetaOf() {
        return annotationData.getMetaOf().map(MergedAnnotation::new);
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
     * Return annotation attribute mappings
     */
    public AnnotationAttributeMapping getAnnotationMapping() {
        return annotationMapping;
    }

    /**
     * Reference to parent annotation
     */
    public Optional<MergedAnnotation> getParent() {
        return Optional.ofNullable(parent);
    }

    /**
     * Reference to root annotation
     */
    public MergedAnnotation getRoot() {
        return root;
    }

    /**
     * 🔍 Retrieves the first {@link MergedAnnotation} of the specified type,
     * including meta-annotations.
     *
     * @param type the annotation type to search for
     * @return optional merged annotation of that type
     */
    public Optional<MergedAnnotation> getAnnotation(Class<? extends Annotation> type) {
        Matcher<Annotation> matcher = AnnotationMatcher.isAnnotation(type);
        return Streamable.findFirst(getFlattened(), ma -> matcher.matches(ma.getNativeAnnotation()));
    }

    /**
     * 📚 Retrieves all {@link MergedAnnotation}s of the specified type,
     * including direct and meta-level matches.
     *
     * @param type the annotation type to filter by
     * @return list of all matching merged annotations
     */
    public List<MergedAnnotation> getAnnotations(Class<? extends Annotation> type) {
        Matcher<Annotation> matcher = AnnotationMatcher.isAnnotation(type);
        return Streamable.findAll(getFlattened(), ma -> matcher.matches(ma.getNativeAnnotation()));
    }

    /**
     * 🎯 Returns the native annotation instance of the given type.
     * <p>
     * If multiple annotations of this type are present, returns the first one found.
     * </p>
     *
     * @param <A>  the annotation type
     * @param type the class of the annotation
     * @return native annotation instance
     * @throws IllegalStateException if not found
     */
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getNativeAnnotation(Class<A> type) {
        Optional<MergedAnnotation> mergedAnnotation = getAnnotation(type);

        mergedAnnotation.orElseThrow(() -> new IllegalStateException("No annotation found for: " + type));

        return (A) mergedAnnotation.get().getNativeAnnotation();
    }

    /**
     * 🎯 Returns all native annotation instances of the given type.
     *
     * @param <A>  the annotation type
     * @param type the annotation class
     * @return list of annotation instances
     */
    @SuppressWarnings("unchecked")
    public <A extends Annotation> List<A> getNativeAnnotations(Class<A> type) {
        return (List<A>) getAnnotations(type).stream().map(MergedAnnotation::getNativeAnnotation).toList();
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
        return getAnnotation(type).isPresent();
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

    public <A extends Annotation> A createSynthesizedAnnotation(Class<A> type) {
        return SynthesizedAnnotationProxy.create(this, type);
    }

    @Override
    public String toString() {
        return "@%s (depth=%d) on %s".formatted(getAnnotationType().getSimpleName(), getDepth(), annotationData.annotatedElement());
    }
}
