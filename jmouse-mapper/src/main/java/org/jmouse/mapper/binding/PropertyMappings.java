package org.jmouse.mapper.binding;

import org.jmouse.core.Verify;
import org.jmouse.mapper.MappingContext;

import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The explicit {@link PropertyMapping}s that apply to one {@code (sourceType -> targetType)} pair. 🗂️
 *
 * <p>A strategy resolves this <em>once per object</em> and then asks it per property. That ordering
 * is the point: {@link TypeMappingRegistry#find} consults every registered rule source, and each
 * source scans its rules, so asking it again for every property of every mapped object is what turns
 * a list of ten thousand DTOs into a hundred thousand lookups.</p>
 *
 * <p>Several rules may cover the same pair - a DSL rule and an annotation-derived one, say. They are
 * merged in registration order and the first rule holding a name wins, so a source registered
 * earlier is never overridden by a later one.</p>
 */
public final class PropertyMappings {

    private static final PropertyMappings EMPTY = new PropertyMappings(Map.of(), List.of());

    private final Map<String, PropertyMapping> mappings;
    private final List<MappingAssertion>       assertions;

    private PropertyMappings(Map<String, PropertyMapping> mappings, List<MappingAssertion> assertions) {
        this.mappings = mappings;
        this.assertions = assertions;
    }

    /**
     * What refuses this pair, gathered from every rule source that spoke about it.
     *
     * <p>⚠️ Unlike a property mapping, an assertion is never overridden — the first source to claim a
     * property wins, but every source that wants to refuse the pair gets to. Overriding a refusal would
     * mean one declaration quietly disarming another one written somewhere else.</p>
     *
     * @return the assertions, in the order their sources were consulted
     */
    public List<MappingAssertion> assertions() {
        return assertions;
    }

    /**
     * Resolve and flatten every rule registered for a type pair.
     *
     * @param context mapping context holding the registry
     * @param sourceType source type
     * @param targetType target type
     * @return the mappings covering this pair, possibly empty
     * @throws IllegalArgumentException if {@code context} is {@code null}
     */
    public static PropertyMappings resolve(MappingContext context, Class<?> sourceType, Class<?> targetType) {
        List<TypeMappingRule> rules = Verify.nonNull(context, "context")
                .mappingRegistry().find(sourceType, targetType, context);

        if (rules.isEmpty()) {
            return EMPTY;
        }

        Map<String, PropertyMapping> merged     = new LinkedHashMap<>();
        List<MappingAssertion>       assertions = new ArrayList<>();

        for (TypeMappingRule rule : rules) {
            if (rule != null) {
                rule.mappings().forEach(merged::putIfAbsent);
                assertions.addAll(rule.assertions());
            }
        }

        return merged.isEmpty() && assertions.isEmpty()
                ? EMPTY
                : new PropertyMappings(Collections.unmodifiableMap(merged), List.copyOf(assertions));
    }

    /**
     * The mapping declared for a target property.
     *
     * @param targetName target property name
     * @return the mapping, or {@code null} when the property has none
     */
    public PropertyMapping find(String targetName) {
        return mappings.get(targetName);
    }

    /**
     * Whether any property is covered.
     *
     * @return {@code true} when nothing is declared for this pair
     */
    public boolean isEmpty() {
        return mappings.isEmpty();
    }

    /**
     * Walk every declared mapping in resolution order.
     *
     * @param consumer receives target property name and its mapping
     */
    public void forEach(BiConsumer<String, PropertyMapping> consumer) {
        mappings.forEach(consumer);
    }
}
