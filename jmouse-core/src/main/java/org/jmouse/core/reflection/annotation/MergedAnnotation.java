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

    public Map<String, Object> toAttributeMap() {
        return toAttributeMap(false);
    }

    public Map<String, Object> toAttributeMap(boolean includeDefaults) {
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

    public Map<String, Object> toResolvedAttributeMap() {
        Map<String, Object>        result     = new LinkedHashMap<>();
        AnnotationAttributeMapping mapping = getAnnotationMapping();

        for (Method attribute : mapping.getAttributes().getAttributes()) {
            @SuppressWarnings("unchecked")
            Object attributeValue = mapping.getAttributeValue(attribute.getName(), (Class<Object>) attribute.getReturnType());
            result.put(attribute.getName(), attributeValue);
        }

        return Collections.unmodifiableMap(result);
    }

    public Map<String, Object> toMergedQualifiedMap() {
        return toMergedQualifiedMap(MergePolicy.FIRST_WINS, false);
    }

    public Map<String, Object> toMergedQualifiedMap(MergePolicy policy, boolean includeDefaults) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (MergedAnnotation mergedAnnotation : getFlattened()) {
            AnnotationAttributeMapping  annotationMapping = mergedAnnotation.getAnnotationMapping();
            Class<? extends Annotation> annotationType    = mergedAnnotation.getAnnotationType();

            for (Method method : annotationMapping.getAttributes().getAttributes()) {
                Object  value     = annotationMapping.getAttributeValue(method);
                boolean isDefault = annotationMapping.isDefaultValue(method, value);

                if (!includeDefaults && isDefault) {
                    continue;
                }

                String key = annotationType.getName() + "#" + method.getName();
                apply(result, key, value, policy);
            }

        }

        return Collections.unmodifiableMap(result);
    }

    public <A extends Annotation> Map<String, Object> toResolvedMapFor(Class<A> targetType) {
        Map<String, Object>        result     = new LinkedHashMap<>();
        AnnotationAttributes       attributes = AnnotationAttributes.forAnnotationType(targetType);
        AnnotationAttributeMapping mapping    = new AnnotationMapping(getNativeAnnotation(), getRoot());

        for (Method attribute : attributes.getAttributes()) {
            @SuppressWarnings("unchecked")
            Object value = mapping.getAttributeValue(attribute.getName(), (Class<Object>) attribute.getReturnType());
            result.put(attribute.getName(), value);
        }

        return Collections.unmodifiableMap(result);
    }

    @SuppressWarnings("unchecked")
    private static void apply(Map<String, Object> result, String key, Object value, MergePolicy policyStrategy) {
        switch (policyStrategy) {
            case COLLECT -> {
                Object previous = result.get(key);
                if (previous == null) {
                    List<Object> collection = new ArrayList<>();
                    collection.add(value);
                    result.put(key, collection);
                } else if (previous instanceof List<?> collection) {
                    ((List<Object>) collection).add(value);
                } else {
                    List<Object> collection = new ArrayList<>();
                    collection.add(previous);
                    collection.add(value);
                    result.put(key, collection);
                }
            }
            case LAST_WINS
                    -> result.put(key, value);
            case FIRST_WINS
                    -> result.putIfAbsent(key, value);
        }
    }

    /**
     * 🧾 Resolved view for the *visible* annotation (this.getAnnotationType())
     * AND optionally its meta annotations, merged into a single unqualified map.
     *
     * <p>Example for {@code @PreAuthorize(...)} meta-annotated with {@code @Authorize}:
     * result = { value, index } from PreAuthorize (resolved),
     * plus { phase } from Authorize (its direct meta) if not already present.</p>
     *
     * <p>Keys are plain attribute names. Use {@link CollisionPolicy#KEEP_EXISTING} to
     * prioritize the visible annotation; other policies are available when names collide.</p>
     */
    public Map<String, Object> toResolvedAttributeMapWithMetas() {
        return toResolvedAttributeMapWithMetas(MetaScope.DIRECT_ONLY, CollisionPolicy.KEEP_EXISTING, false);
    }

    /**
     * Same as {@link #toResolvedAttributeMapWithMetas()}, with controls.
     *
     * @param scope whether to include only the direct meta or all meta levels
     * @param collisionPolicy how to handle key collisions (same attribute name)
     * @param includeDefaults include attributes equal to their default values
     */
    public Map<String, Object> toResolvedAttributeMapWithMetas(
            MetaScope scope,
            CollisionPolicy collisionPolicy,
            boolean includeDefaults
    ) {
        // 1) Start with the resolved map for the *visible* type (this annotation)
        Map<String, Object> result = new LinkedHashMap<>(toResolvedAttributeMap());

        // 2) Collect meta annotations to include
        List<MergedAnnotation> metas = switch (scope) {
            case DIRECT_ONLY
                    -> getParent().map(List::of).orElse(List.of());
            case ALL -> {
                List<MergedAnnotation> flattened = getFlattened();
                yield flattened.size() <= 1 ? List.of() : flattened.subList(1, flattened.size());
            }
        };

        // 3) For each meta, add its *own* direct attribute values (or resolved-for-self)
        for (MergedAnnotation meta : metas) {
            // direct map: respects defaults flag, no alias remap into our type
            Map<String, Object> metaMap = meta.toAttributeMap(includeDefaults);
            // You could also use: meta.toResolvedMapFor(meta.getAnnotationType()) if you want
            // alias-resolution within that meta type itself. Direct map is usually enough here.
            for (Map.Entry<String, Object> entry : metaMap.entrySet()) {
                String key   = entry.getKey();
                Object value = entry.getValue();

                switch (collisionPolicy) {
                    case KEEP_EXISTING -> result.putIfAbsent(key, value);
                    case OVERWRITE -> result.put(key, value);
                    case QUALIFY_META_KEY -> {
                        String unique = key;

                        if (result.containsKey(key)) {
                            var    type      = meta.getAnnotationType();
                            String qualified = key + "@" + type.getSimpleName();
                            int    index     = 0;
                            while (result.containsKey(unique)) {
                                unique = qualified + "#" + index++;
                            }
                            result.put(unique, value);
                        }

                        result.put(unique, value);
                    }
                }
            }
        }

        return Collections.unmodifiableMap(result);
    }

    /**
     * 🎯 Same concept, but targeted to an arbitrary "visible" type.
     * Builds the resolved map for {@code targetType} as if it were the visible annotation,
     * then augments it with meta attributes per the given scope/policy.
     */
    public <A extends Annotation> Map<String, Object> toResolvedAttributeMapForWithMetas(
            Class<A> targetType,
            MetaScope scope,
            CollisionPolicy collisionPolicy,
            boolean includeDefaults
    ) {
        // Start with how targetType looks after full resolve through this merged root
        Map<String, Object> result = new LinkedHashMap<>(toResolvedMapFor(targetType));

        // Add metas (same selection as above)
        List<MergedAnnotation> metas = switch (scope) {
            case DIRECT_ONLY -> getParent().map(List::of).orElse(List.of());
            case ALL -> {
                List<MergedAnnotation> flat = getFlattened();
                yield flat.size() <= 1 ? List.of() : flat.subList(1, flat.size());
            }
        };

        for (MergedAnnotation meta : metas) {
            Map<String, Object> metaMap = meta.toAttributeMap(includeDefaults);
            for (Map.Entry<String, Object> e : metaMap.entrySet()) {
                String key   = e.getKey();
                Object value = e.getValue();
                switch (collisionPolicy) {
                    case KEEP_EXISTING -> result.putIfAbsent(key, value);
                    case OVERWRITE -> result.put(key, value);
                    case QUALIFY_META_KEY -> {
                        String unique = key;

                        if (result.containsKey(key)) {
                            var    type      = meta.getAnnotationType();
                            String qualified = key + "@" + type.getSimpleName();
                            int    index     = 0;
                            while (result.containsKey(unique)) {
                                unique = qualified + "#" + index++;
                            }
                            result.put(unique, value);
                        }

                        result.put(unique, value);
                    }
                }
            }
        }

        return Collections.unmodifiableMap(result);
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
