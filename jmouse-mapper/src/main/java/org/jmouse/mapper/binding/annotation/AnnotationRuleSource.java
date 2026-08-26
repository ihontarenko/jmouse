package org.jmouse.mapper.binding.annotation;

import org.jmouse.core.access.descriptor.MethodDescriptor;
import org.jmouse.core.access.descriptor.structured.DescriptorResolver;
import org.jmouse.core.access.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.access.descriptor.structured.PropertyDescriptor;
import org.jmouse.mapper.MappingContext;
import org.jmouse.mapper.binding.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link TypeMappingRuleSource} that derives {@link TypeMappingRule}s from annotations declared
 * on the target type's accessor methods. 🏷️
 *
 * <p>This source introspects the {@code targetType} structure via {@link DescriptorResolver}
 * and scans both the getter and the setter of each property to produce {@link PropertyMapping}
 * entries. A property that has only one of the two is read through whichever exists.</p>
 *
 * <h3>Supported annotations</h3>
 * <ul>
 *   <li>{@link MappingIgnore} - ignores a target property</li>
 *   <li>{@link MappingReference} - reads value from a source reference/path</li>
 *   <li>{@link MappingConstant} - assigns a constant value</li>
 * </ul>
 *
 * <h3>Caching</h3>
 * <p>Introspection results are memoized per {@code (sourceType, targetType)}, including the common
 * "this type carries no mapping annotations" answer. Caching that absence matters: without it every
 * property of every mapped object re-reads the whole target descriptor.</p>
 *
 * <p><strong>Note:</strong> the {@link MappingContext} argument is currently not used for computation,
 * but is part of the {@link TypeMappingRuleSource} contract and allows future context-aware behavior.</p>
 */
public final class AnnotationRuleSource implements TypeMappingRuleSource {

    private static final Annotation[] NO_ANNOTATIONS = new Annotation[0];

    private final ConcurrentHashMap<CacheKey, Optional<TypeMappingRule>> cache = new ConcurrentHashMap<>();

    /**
     * Find (or compute) a {@link TypeMappingRule} for the given type pair.
     *
     * <p>Rules are computed once and cached for subsequent lookups.</p>
     *
     * @param sourceType source type
     * @param targetType target type
     * @param context mapping context (currently unused)
     * @return computed rule, or {@code null} when no relevant annotations are present
     */
    @Override
    public TypeMappingRule find(Class<?> sourceType, Class<?> targetType, MappingContext context) {
        return cache.computeIfAbsent(
                new CacheKey(sourceType, targetType),
                ignored -> Optional.ofNullable(compute(sourceType, targetType))
        ).orElse(null);
    }

    /**
     * Compute a {@link TypeMappingRule} by scanning the target type descriptor and reading
     * mapping annotations from property accessor methods.
     *
     * @param sourceType source type
     * @param targetType target type
     * @return rule instance, or {@code null} when no mappings are discovered
     */
    @SuppressWarnings("unchecked")
    private TypeMappingRule compute(Class<?> sourceType, Class<?> targetType) {
        Map<String, PropertyMapping> mappings   = new LinkedHashMap<>();
        ObjectDescriptor<Object>     descriptor = (ObjectDescriptor<Object>) DescriptorResolver.describe(targetType);

        for (PropertyDescriptor<Object> property : descriptor.getProperties().values()) {
            PropertyMapping propertyMapping = readMapping(property);

            if (propertyMapping != null) {
                mappings.put(property.getName(), propertyMapping);
            }
        }

        if (mappings.isEmpty()) {
            return null;
        }

        return new TypeMappingRule(sourceType, targetType, mappings);
    }

    /**
     * Translate the annotations on a property's accessors into a {@link PropertyMapping}.
     *
     * <p>The setter is consulted first, then the getter. Either may be absent, and a descriptor
     * flavour that exposes neither simply contributes nothing.</p>
     *
     * @param property target property descriptor
     * @return mapping derived from annotations, or {@code null} when none match
     */
    private PropertyMapping readMapping(PropertyDescriptor<?> property) {
        String          targetName = property.getName();
        PropertyMapping mapping    = readMapping(targetName, annotationsOf(property, true));

        if (mapping != null) {
            return mapping;
        }

        return readMapping(targetName, annotationsOf(property, false));
    }

    /**
     * Read the annotations of one accessor, tolerating a property that does not expose it.
     *
     * @param property target property descriptor
     * @param setter {@code true} to read the setter, {@code false} to read the getter
     * @return declared annotations, never {@code null}
     */
    private Annotation[] annotationsOf(PropertyDescriptor<?> property, boolean setter) {
        MethodDescriptor accessor;

        try {
            accessor = setter ? property.getSetterMethod() : property.getGetterMethod();
        } catch (UnsupportedOperationException exception) {
            return NO_ANNOTATIONS;
        }

        Method method = accessor == null ? null : accessor.unwrap();

        return method == null ? NO_ANNOTATIONS : method.getAnnotations();
    }

    /**
     * Translate accessor annotations into a {@link PropertyMapping}.
     *
     * <p>If multiple supported annotations are present, the first matching annotation in the
     * provided array wins (iteration order matters).</p>
     *
     * @param targetName target property name
     * @param annotations annotations declared on the accessor method
     * @return mapping derived from annotations, or {@code null} when none match
     */
    private PropertyMapping readMapping(String targetName, Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof MappingIgnore) {
                return new PropertyMapping.Ignore(targetName);
            }
            if (annotation instanceof MappingReference reference) {
                return new PropertyMapping.Reference(targetName, reference.value());
            }
            if (annotation instanceof MappingConstant constant) {
                return new PropertyMapping.Constant(targetName, constant.value());
            }
        }
        return null;
    }

    /**
     * Cache key for memoizing mapping rules per type pair.
     *
     * @param sourceType source type
     * @param targetType target type
     */
    private record CacheKey(Class<?> sourceType, Class<?> targetType) {}
}
