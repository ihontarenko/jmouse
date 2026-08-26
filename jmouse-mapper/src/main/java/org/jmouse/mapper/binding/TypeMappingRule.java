package org.jmouse.mapper.binding;

import org.jmouse.core.Verify;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable mapping rule for a specific {@code (sourceType -> targetType)} pair. 🧩
 *
 * <p>{@code TypeMappingRule} holds a set of {@link PropertyMapping} instructions keyed by
 * target property name. The mapping engine uses these bindings to decide how each target
 * property should be populated (reference, constant, compute, valueProvider, ignore, ...).</p>
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
    private final List<MappingAssertion>       assertions;
    private final WholeTargetMapping           whole;

    /**
     * A pair converted <em>whole</em> — one expression whose result is the target itself.
     *
     * <h3>⚠️ Why this is not a {@link PropertyMapping}</h3>
     *
     * <p>Every {@link PropertyMapping} is keyed by a target property name, because filling a property
     * is what it describes. A whole-pair conversion names none: a value object, a money type or an
     * identifier wrapper is built in one step, and the expression's result <em>is</em> the object. So
     * it sits beside the property mappings rather than inside them — a rule that carries one has no
     * properties to fill, and a rule that fills properties has no whole.</p>
     *
     * @param expression the expression exactly as written, kept so a translator can render the rule
     *                   back and a failure can quote it
     * @param function   what produces the target from the source
     */
    public record WholeTargetMapping(String expression, ComputeFunction<Object> function) {
    }

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
        this(sourceType, targetType, mappings, List.of());
    }

    /**
     * Create a mapping rule that also refuses the pair under some conditions.
     *
     * @param sourceType source type
     * @param targetType target type
     * @param mappings   how each target property is filled
     * @param assertions what stops the mapping happening at all
     */
    public TypeMappingRule(
            Class<?> sourceType,
            Class<?> targetType,
            Map<String, PropertyMapping> mappings,
            List<MappingAssertion> assertions
    ) {
        this(sourceType, targetType, mappings, assertions, null);
    }

    /**
     * Create a rule that converts the pair whole rather than property by property.
     *
     * @param sourceType source type
     * @param targetType target type
     * @param assertions what stops the mapping happening at all
     * @param whole      the expression whose result is the target
     * @return the rule
     */
    public static TypeMappingRule converting(
            Class<?> sourceType,
            Class<?> targetType,
            List<MappingAssertion> assertions,
            WholeTargetMapping whole
    ) {
        return new TypeMappingRule(sourceType, targetType, Map.of(), assertions,
                                   Verify.nonNull(whole, "whole"));
    }

    private TypeMappingRule(
            Class<?> sourceType,
            Class<?> targetType,
            Map<String, PropertyMapping> mappings,
            List<MappingAssertion> assertions,
            WholeTargetMapping whole
    ) {
        this.sourceType = Verify.nonNull(sourceType, "sourceType");
        this.targetType = Verify.nonNull(targetType, "targetType");
        this.mappings = Collections.unmodifiableMap(new LinkedHashMap<>(mappings));
        this.assertions = List.copyOf(assertions);
        this.whole = whole;
    }

    /**
     * The expression that builds the target in one step, or {@code null} where this rule fills
     * properties.
     *
     * @return the whole-target mapping, or {@code null}
     */
    public WholeTargetMapping whole() {
        return whole;
    }

    /**
     * Whether this rule replaces the mapping rather than describing it.
     *
     * @return {@code true} when the pair converts whole
     */
    public boolean mapsWhole() {
        return whole != null;
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


    /**
     * What refuses this pair, and when.
     *
     * @return the assertions, possibly empty
     */
    public List<MappingAssertion> assertions() {
        return assertions;
    }

    /**
     * Return the internal mappings map.
     *
     * <p>This method is intended for internal use only.</p>
     *
     * @return mappings indexed by target property name
     */
    public Map<String, PropertyMapping> mappings() {
        return mappings;
    }

}
