package org.jmouse.core.mapping.binding;

import org.jmouse.core.Verify;
import org.jmouse.core.mapping.MappingContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

/**
 * DSL builder for configuring a {@link PropertyMapping} for a single target property. 🧩
 *
 * <p>{@code PropertyMappingBuilder} starts with a default "same-name reference" mapping:
 * {@code reference(targetName -> targetName)}.</p>
 *
 * <p>Each DSL call replaces or decorates the current mapping (depending on the operation),
 * allowing you to compose rules such as:</p>
 * <ul>
 *   <li>base mapping: reference/provider/compute/constant/ignore</li>
 *   <li>decorators: {@code defaultValue}, {@code transform}, {@code when}, {@code required}, {@code coalesce}</li>
 * </ul>
 *
 * <p>The builder is stateful and intended for one property at a time.</p>
 */
public final class PropertyMappingBuilder {

    private final String          name;
    private       PropertyMapping current;

    /**
     * Create a builder for a specific target property name.
     *
     * <p>Initial mapping is {@code reference(name, name)} (same-name reference).</p>
     *
     * @param name target property name (must not be blank)
     */
    public PropertyMappingBuilder(String name) {
        this.name = Verify.notBlank(name, "targetName");
        this.current = PropertyMapping.reference(name, name);
    }

    /**
     * Target property name this builder configures.
     *
     * @return target property name
     */
    public String targetName() {
        return name;
    }

    /**
     * Build the resulting {@link PropertyMapping}.
     *
     * @return current property mapping
     */
    public PropertyMapping build() {
        return current;
    }

    /**
     * Replace current mapping with a reference mapping using {@code sourceReference}.
     *
     * @param sourceReference source accessor reference/path
     * @return this builder
     */
    public PropertyMappingBuilder reference(String sourceReference) {
        this.current = PropertyMapping.reference(name, sourceReference);
        return this;
    }

    /**
     * Replace current mapping with a provider mapping.
     *
     * @param provider value provider (invoked with the current source object)
     * @return this builder
     */
    public PropertyMappingBuilder provider(ValueProvider<?> provider) {
        @SuppressWarnings("unchecked")
        ValueProvider<Object> valueProvider = (ValueProvider<Object>) provider;
        this.current = PropertyMapping.provider(name, valueProvider);
        return this;
    }

    /**
     * Replace current mapping with a compute mapping.
     *
     * @param function compute function (source + context -> value)
     * @return this builder
     */
    public PropertyMappingBuilder compute(ComputeFunction<?> function) {
        @SuppressWarnings("unchecked")
        ComputeFunction<Object> computeFunction = (ComputeFunction<Object>) function;
        this.current = PropertyMapping.compute(name, computeFunction);
        return this;
    }

    /**
     * Replace current mapping with a constant mapping.
     *
     * @param value constant value (may be {@code null})
     * @return this builder
     */
    public PropertyMappingBuilder constant(Object value) {
        this.current = PropertyMapping.constant(name, value);
        return this;
    }

    /**
     * Replace current mapping with an ignore mapping.
     *
     * @return this builder
     */
    public PropertyMappingBuilder ignore() {
        this.current = PropertyMapping.ignore(name);
        return this;
    }

    /**
     * Decorate current mapping with a default value used when the current mapping resolves to {@code null}.
     *
     * @param value default value
     * @return this builder
     */
    public PropertyMappingBuilder defaultValue(Object value) {
        return defaultValue(() -> value);
    }

    /**
     * Decorate current mapping with a lazily supplied default value used when the current mapping resolves to {@code null}.
     *
     * @param supplier default value supplier
     * @return this builder
     */
    public PropertyMappingBuilder defaultValue(Supplier<?> supplier) {
        this.current = PropertyMapping.defaultValue(name, this.current, supplier);
        return this;
    }

    /**
     * Decorate current mapping with a value transformer.
     *
     * @param transformer transformer applied to the resolved value
     * @return this builder
     */
    public PropertyMappingBuilder transform(ValueTransformer transformer) {
        this.current = PropertyMapping.transform(name, this.current, transformer);
        return this;
    }

    /**
     * Guard current mapping with a condition.
     *
     * <p>If the predicate returns {@code false}, the mapping should behave as if no value was produced
     * (implementation-dependent, usually {@code null}).</p>
     *
     * @param condition predicate evaluated against (source, context)
     * @return this builder
     */
    public PropertyMappingBuilder when(BiPredicate<Object, MappingContext> condition) {
        this.current = PropertyMapping.when(name, condition, this.current);
        return this;
    }

    /**
     * Mark current mapping as required.
     *
     * <p>If the resolved value is missing (typically {@code null}), a mapping error is produced
     * using the provided error code/message.</p>
     *
     * @param code stable error code
     * @param message human-readable message
     * @return this builder
     */
    public PropertyMappingBuilder required(String code, String message) {
        this.current = PropertyMapping.required(name, this.current, code, message);
        return this;
    }

    /**
     * Compose a coalesce mapping that tries the current mapping first, then the provided candidates.
     *
     * @param candidates fallback candidates tried in order
     * @return this builder
     */
    public PropertyMappingBuilder coalesce(PropertyMapping... candidates) {
        List<PropertyMapping> collection = new ArrayList<>();
        collection.add(this.current);
        collection.addAll(Arrays.asList(candidates));
        this.current = PropertyMapping.coalesce(name, collection);
        return this;
    }

    /**
     * Utility factory for building a reference candidate using this builder's target name.
     *
     * @param sourceReference source accessor reference/path
     * @return reference mapping candidate
     */
    public PropertyMapping referenceCandidate(String sourceReference) {
        return PropertyMapping.reference(name, sourceReference);
    }

    /**
     * Utility factory for building a constant candidate using this builder's target name.
     *
     * @param value constant value
     * @return constant mapping candidate
     */
    public PropertyMapping constantCandidate(Object value) {
        return PropertyMapping.constant(name, value);
    }
}
