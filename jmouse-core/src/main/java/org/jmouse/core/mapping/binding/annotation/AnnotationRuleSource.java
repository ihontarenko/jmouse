package org.jmouse.core.mapping.binding.annotation;

import org.jmouse.core.bind.descriptor.structured.DescriptorResolver;
import org.jmouse.core.bind.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.mapping.binding.*;

import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link TypeMappingRuleSource} that derives {@link TypeMappingRule}s from annotations declared
 * on the target type's accessor methods (typically getters). 🏷️
 *
 * <p>This source introspects the {@code targetType} structure via {@link DescriptorResolver}
 * and scans getter method annotations to produce {@link PropertyMapping} entries.</p>
 *
 * <h3>Supported annotations</h3>
 * <ul>
 *   <li>{@link MappingIgnore} - ignores a target property</li>
 *   <li>{@link MappingReference} - reads value from a source reference/path</li>
 *   <li>{@link MappingConstant} - assigns a constant value</li>
 * </ul>
 *
 * <h3>Caching</h3>
 * <p>Computed rules are cached per {@code (sourceType,targetType)} using a thread-safe
 * {@link ConcurrentHashMap}. If no mappings are found, {@code null} is cached as the computed value
 * is represented by the absence of an entry (via {@link ConcurrentHashMap#computeIfAbsent}).</p>
 *
 * <p><strong>Note:</strong> the {@link MappingContext} argument is currently not used for computation,
 * but is part of the {@link TypeMappingRuleSource} contract and allows future context-aware behavior.</p>
 */
public final class AnnotationRuleSource implements TypeMappingRuleSource {

    private final ConcurrentHashMap<CacheKey, TypeMappingRule> cache = new ConcurrentHashMap<>();

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
        return cache.computeIfAbsent(new CacheKey(sourceType, targetType), ignore -> compute(sourceType, targetType));
    }

    /**
     * Compute a {@link TypeMappingRule} by scanning the target type descriptor and reading
     * mapping annotations from property getter methods.
     *
     * @param sourceType source type
     * @param targetType target type
     * @return rule instance, or {@code null} when no mappings are discovered
     */
    @SuppressWarnings("unchecked")
    private TypeMappingRule compute(Class<?> sourceType, Class<?> targetType) {
        Map<String, PropertyMapping> mappings   = new LinkedHashMap<>();
        ObjectDescriptor<Object>     descriptor = (ObjectDescriptor<Object>) DescriptorResolver.describe(targetType);

        descriptor.getProperties().forEach((name, property) -> {
            PropertyMapping propertyMapping = readMapping(name, property.getGetterMethod().unwrap().getAnnotations());
            if (propertyMapping != null) {
                mappings.put(property.getName(), propertyMapping);
            }
        });

        if (mappings.isEmpty()) {
            return null;
        }

        return new TypeMappingRule(sourceType, targetType, mappings);
    }

    /**
     * Translate getter annotations into a {@link PropertyMapping}.
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
