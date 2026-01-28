package org.jmouse.core.mapping.binding;

import org.jmouse.core.Verify;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable mapping rule for a specific {@code (sourceType -> targetType)} pair. 🧩
 *
 * <p>{@code TypeMappingRule} holds a set of {@link PropertyMapping} instructions keyed by
 * target property name. The mapping engine uses these bindings to decide how each target
 * property should be populated (reference, constant, compute, provider, ignore, ...).</p>
 *
 * <h2>Examples</h2>
 * <pre>{@code
 * Map<String, PropertyBinding> bindings = new LinkedHashMap<>();
 * bindings.put("id", PropertyBinding.reference("id", "id"));
 * bindings.put("name", PropertyBinding.reference("name", "fullName"));
 * bindings.put("role", PropertyBinding.constant("role", "USER"));
 * bindings.put("password", PropertyBinding.ignore("password"));
 *
 * TypeMappingRule rule = new TypeMappingRule(UserDto.class, User.class, bindings);
 *
 * PropertyBinding b = rule.find("name"); // Reference("name", "fullName")
 * }</pre>
 *
 * <h3>Notes</h3>
 * <ul>
 *   <li>Bindings are stored in an unmodifiable map; insertion order is preserved.</li>
 *   <li>{@link #find(String)} returns {@code null} when no binding exists for the target name.</li>
 * </ul>
 *
 * @see PropertyMapping
 * @see TypeMappingRegistry
 */
public final class TypeMappingRule {

    private final Class<?>                     sourceType;
    private final Class<?>                     targetType;
    private final Map<String, PropertyMapping> mappings;

    /**
     * Create a mapping rule for a specific type pair.
     *
     * <p>The provided {@code bindings} map is defensively copied into a {@link LinkedHashMap}
     * and wrapped with {@link Collections#unmodifiableMap(Map)}.</p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * TypeMappingRule rule = new TypeMappingRule(
     *     UserDto.class,
     *     User.class,
     *     Map.of(
     *         "id", PropertyBinding.reference("id", "id"),
     *         "status", PropertyBinding.constant("status", "ACTIVE")
     *     )
     * );
     * }</pre>
     *
     * @param sourceType source type this rule applies to
     * @param targetType target type this rule applies to
     * @param mappings bindings keyed by target property name
     * @throws IllegalArgumentException if {@code sourceType} or {@code targetType} is {@code null}
     * @throws NullPointerException if {@code bindings} is {@code null} (caller responsibility)
     */
    public TypeMappingRule(Class<?> sourceType, Class<?> targetType, Map<String, PropertyMapping> mappings) {
        this.sourceType = Verify.nonNull(sourceType, "sourceType");
        this.targetType = Verify.nonNull(targetType, "targetType");
        this.mappings = Collections.unmodifiableMap(new LinkedHashMap<>(mappings));
    }

    /**
     * Source type this rule is defined for.
     *
     * @return source type (never {@code null})
     */
    public Class<?> sourceType() {
        return sourceType;
    }

    /**
     * Target type this rule is defined for.
     *
     * @return target type (never {@code null})
     */
    public Class<?> targetType() {
        return targetType;
    }

    /**
     * Find the {@link PropertyMapping} for the given target property name.
     *
     * <h3>Example</h3>
     * <pre>{@code
     * PropertyBinding binding = rule.find("email");
     * if (binding == null) {
     *     // no explicit binding -> fall back to default conventions
     * }
     * }</pre>
     *
     * @param targetName target property name
     * @return binding for {@code targetName}, or {@code null} if not present
     */
    public PropertyMapping find(String targetName) {
        return mappings.get(targetName);
    }

}
