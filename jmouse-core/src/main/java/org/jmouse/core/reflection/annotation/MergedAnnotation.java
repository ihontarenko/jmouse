package org.jmouse.core.reflection.annotation;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.Streamable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
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
     * Annotation's attribute value
     *
     * @return underlying annotation's attribute value
     */
    public Object getValue(String name) {
        return getAnnotationMapping().getAttributeValue(name, Object.class);
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
     * Reference to sourceRoot annotation
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
     * 📚 Flatten annotation hierarchy (breadth-first).
     *
     * <p>Returns a list of this annotation and all its meta-annotations in breadth-first order.
     * Used for scanning or attribute resolution across layers.
     *
     * @return flat list of annotations starting from this
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

    /**
     * 🗺️ Returns a map of this annotation's own attributes.
     *
     * <p>By default, attributes whose values equal their declared defaults are omitted.
     * Use {@link #asMap(boolean)} to include them.</p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * @PreAuthorize(value = "33 % 3 == 0", index = 666)
     * // For the MergedAnnotation representing @PreAuthorize:
     * Map<String,Object> m = merged.asMap();
     * // => { value: "33 % 3 == 0", index: 666 }   // default-valued attrs are skipped
     * }</pre>
     *
     * @return unmodifiable map of attributeName → value for this annotation instance
     */
    public Map<String, Object> asMap() {
        return asMap(false);
    }

    /**
     * 🗺️ Returns a map of this annotation's own attributes with control over defaults.
     *
     * <p>If {@code includeDefaults} is {@code true}, attributes whose values equal their
     * declared defaults are included; otherwise they are omitted.</p>
     *
     * @param includeDefaults whether to include attributes equal to their default values
     * @return unmodifiable map of attributeName → value for this annotation instance
     */
    public Map<String, Object> asMap(boolean includeDefaults) {
        Map<String, Object>        result  = new LinkedHashMap<>();
        AnnotationAttributeMapping mapping = getAnnotationMapping();

        for (Method attribute : mapping.getAttributes().getAttributes()) {
            Object  attributeValue = mapping.getAttributeValue(attribute);
            boolean isDefault      = mapping.isDefaultValue(attribute, attributeValue);
            if (includeDefaults || !isDefault) {
                result.put(attribute.getName(), attributeValue);
            }
        }

        return Collections.unmodifiableMap(result);
    }

    /**
     * ✅ Resolved view for the <em>visible</em> annotation + its direct meta-annotation.
     *
     * <p>Builds a flat, unqualified map starting from {@link #asMap()} (this annotation only),
     * then merges attributes from the <strong>direct</strong> meta according to
     * {@link CollisionPolicy#KEEP_EXISTING} (visible annotation wins). Default-valued attributes
     * from metas are skipped.</p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * @PreAuthorize(value = "33 % 3 == 0", index = 666)
     * @Authorize(phase = Phase.BEFORE)   // meta on @PreAuthorize
     *
     * Map<String,Object> m = merged.asResolved();
     * // => { value: "33 % 3 == 0", index: 666, phase: BEFORE }
     * }</pre>
     *
     * @return unmodifiable merged map of attributeName → value
     */
    public Map<String, Object> asResolved() {
        return asResolved(MetaScope.DIRECT_ONLY, CollisionPolicy.KEEP_EXISTING, false);
    }

    /**
     * 🌐 Resolved view for the visible annotation + <em>all</em> meta-annotations (breadth-first).
     *
     * <p>Equivalent to {@code asResolved(MetaScope.ALL, KEEP_EXISTING, includeDefaults=false)}.</p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * // If there are multiple meta levels (A -> B -> C), this collects B and C.
     * Map<String,Object> m = merged.asResolvedAll();
     * }</pre>
     *
     * @return unmodifiable merged map of attributeName → value
     */
    public Map<String, Object> asResolvedAll() {
        return asResolved(MetaScope.ALL, CollisionPolicy.KEEP_EXISTING, true);
    }

    /**
     * 🧩 Resolved view for the visible annotation plus metas with full control.
     *
     * <p>Starts from {@link #asMap()} (this annotation only) and merges meta-annotation attributes
     * in breadth-first order (as returned by {@link #metasOf(MetaScope)}).</p>
     *
     * <ul>
     *   <li><b>scope</b> — which meta levels to include ({@code DIRECT_ONLY} or {@code ALL}).</li>
     *   <li><b>policy</b> — how to handle collisions for the same key:
     *     <ul>
     *       <li>{@link CollisionPolicy#KEEP_EXISTING} — keep existing value (visible annotation wins)</li>
     *       <li>{@link CollisionPolicy#OVERWRITE} — meta value overwrites visible one</li>
     *     </ul>
     *   </li>
     *   <li><b>includeDefaults</b> — whether to include default-valued attributes from metas.</li>
     * </ul>
     *
     * <h3>Examples</h3>
     * <pre>{@code
     * // Direct meta only, visible wins (skip meta defaults):
     * Map<String,Object> m1 = merged.asResolved(MetaScope.DIRECT_ONLY, CollisionPolicy.KEEP_EXISTING, false);
     *
     * // All metas, allow overwrite on conflicts, include meta defaults:
     * Map<String,Object> m2 = merged.asResolved(MetaScope.ALL, CollisionPolicy.OVERWRITE, true);
     * }</pre>
     *
     * @param scope meta-annotation scope to include
     * @param policy collision policy for same-named attributes
     * @param includeDefaults include attributes equal to their default values from metas
     * @return unmodifiable merged map of attributeName → value
     */
    public Map<String, Object> asResolved(MetaScope scope, CollisionPolicy policy, boolean includeDefaults) {
        return merge(asMap(includeDefaults), metasOf(scope), policy, includeDefaults);
    }

    /**
     * 🧪 Merges a base attribute map with meta-annotation attribute maps.
     *
     * <p>This is the internal workhorse used by {@link #asResolved(MetaScope, CollisionPolicy, boolean)}.
     * Iterates metas in breadth-first order and applies the given {@link CollisionPolicy}.</p>
     *
     * @param base base attributes (usually {@link #asMap()})
     * @param metas meta-annotations to merge (from {@link #metasOf(MetaScope)})
     * @param policy collision handling strategy
     * @param includeDefaults include default-valued meta attributes
     * @return unmodifiable merged map
     */
    private Map<String, Object> merge(
            Map<String, Object> base, List<MergedAnnotation> metas, CollisionPolicy policy, boolean includeDefaults) {
        Map<String, Object> result = new LinkedHashMap<>(base);

        for (MergedAnnotation meta : metas) {
            Map<String, Object> metaMap = meta.asMap(includeDefaults);
            for (Map.Entry<String, Object> entry : metaMap.entrySet()) {
                switch (policy) {
                    case KEEP_EXISTING -> result.putIfAbsent(entry.getKey(), entry.getValue());
                    case OVERWRITE     -> result.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return Collections.unmodifiableMap(result);
    }

    /**
     * 🧭 Selects which meta-annotations to include for merging.
     *
     * <p>Returns metas in breadth-first order as produced by {@link #getFlattened()}:
     * <ul>
     *   <li>{@link MetaScope#DIRECT_ONLY} — a single-element list with the direct parent (if any).</li>
     *   <li>{@link MetaScope#ALL} — all meta levels except self; may be empty.</li>
     * </ul>
     * </p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * List<MergedAnnotation> direct = merged.metasOf(MetaScope.DIRECT_ONLY); // size 0 or 1
     * List<MergedAnnotation> all    = merged.metasOf(MetaScope.ALL);         // 0..N
     * }</pre>
     *
     * @param scope meta scope selector
     * @return list of meta-annotations to use for merging (never {@code null})
     */
    public List<MergedAnnotation> metasOf(MetaScope scope) {
        return switch (scope) {
            case DIRECT_ONLY -> getParent().map(List::of).orElse(List.of());
            case ALL -> getRoot().getFlattened();
        };
    }


    /**
     * 🌀 Synthesize this merged annotation into a runtime proxy.
     *
     * <p>Returns a dynamic proxy implementing the original annotation type.
     * Values are resolved via this merged context.
     *
     * @param <A> annotation type
     * @return synthesized annotation proxy
     */
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A synthesize() {
        return (A) SynthesizedAnnotationProxy.create(this, getAnnotationType());
    }

    /**
     * 🌀 Synthesize this merged annotation as a specific type.
     *
     * @param type annotation type to synthesize
     * @param <A> annotation type
     * @return synthesized annotation proxy
     */
    public <A extends Annotation> A synthesize(Class<A> type) {
        return SynthesizedAnnotationProxy.create(this, type);
    }

    /**
     * 🔍 String view of this merged annotation.
     *
     * <p>Example: <code>@MyAnnotation (depth=1) on class my.Controller</code>
     *
     * @return compact string representation
     */
    @Override
    public String toString() {
        return "@%s (depth=%d) on %s"
                .formatted(getAnnotationType().getSimpleName(), getDepth(), annotationData.annotatedElement());
    }
}
