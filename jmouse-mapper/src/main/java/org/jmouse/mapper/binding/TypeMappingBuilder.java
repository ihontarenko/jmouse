package org.jmouse.mapper.binding;

import org.jmouse.core.Customizer;
import org.jmouse.mapper.MappingContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import static org.jmouse.core.Verify.nonNull;
import static org.jmouse.core.Verify.notBlank;

/**
 * Fluent builder for defining a {@link TypeMappingRule} between {@code S -> T}. 🧱
 *
 * <p>Properties that share a name on both sides need no entry here at all - the engine reads them
 * by name. Declare only the ones that disagree.</p>
 *
 * <pre>{@code
 * Mappers.mapper(UserA.class, UserB.class, user -> user
 *         .rename("dateOfBirth", "birthDay")     // source name -> target name
 *         .constant("role", "USER")
 *         .ignore("password"));
 * }</pre>
 *
 * <p>⚠️ <b>Every method takes the TARGET property name first</b>, because a rule is a statement
 * about how a target slot is filled. {@link #rename(String, String)} is the one exception and says
 * so in its name: it reads source-to-target, the direction the value travels.</p>
 *
 * <p>{@link #property(String, Customizer)} is the full form and reaches the whole
 * {@link PropertyMapping} algebra - conditions, defaults, transformers, coalescing. The other
 * methods are shorthands for its common shapes.</p>
 *
 * @param <S> source type
 * @param <T> target type
 */
public final class TypeMappingBuilder<S, T> {

    private final Class<S>                     sourceType;
    private final Class<T>                     targetType;
    private final Map<String, PropertyMapping> bindings = new LinkedHashMap<>();

    /**
     * Creates a builder for mappings from {@code sourceType} to {@code targetType}.
     *
     * @param sourceType source type
     * @param targetType target type
     */
    public TypeMappingBuilder(Class<S> sourceType, Class<T> targetType) {
        this.sourceType = nonNull(sourceType, "sourceType");
        this.targetType = nonNull(targetType, "targetType");
    }

    /**
     * Configures mapping for a single target property, with the full DSL available.
     *
     * @param targetName target property name
     * @param customizer property mapping customizer
     *
     * @return this builder
     */
    public TypeMappingBuilder<S, T> property(String targetName, Customizer<PropertyMappingBuilder> customizer) {
        PropertyMappingBuilder builder = new PropertyMappingBuilder(notBlank(targetName, "targetName"));
        nonNull(customizer, "customizer").customize(builder);
        bindings.put(targetName, builder.build());
        return this;
    }

    /**
     * Fills a target property from a differently named source property.
     *
     * <p>Reads in the direction the value travels: {@code rename("dateOfBirth", "birthDay")} takes
     * the source's {@code dateOfBirth} and writes the target's {@code birthDay}. The source side may
     * be a path rather than a plain name - {@code rename("buyer.email", "contactEmail")}.</p>
     *
     * @param sourceName source property name or path to read from
     * @param targetName target property name to fill
     *
     * @return this builder
     */
    public TypeMappingBuilder<S, T> rename(String sourceName, String targetName) {
        return property(targetName, builder -> builder.reference(sourceName));
    }

    /**
     * Leaves a target property untouched, whatever the source holds.
     *
     * @param targetName target property name
     *
     * @return this builder
     */
    public TypeMappingBuilder<S, T> ignore(String targetName) {
        return property(targetName, PropertyMappingBuilder::ignore);
    }

    /**
     * Assigns a fixed value to a target property.
     *
     * @param targetName target property name
     * @param value      constant value
     *
     * @return this builder
     */
    public TypeMappingBuilder<S, T> constant(String targetName, Object value) {
        return property(targetName, builder -> builder.constant(value));
    }

    /**
     * Fills a target property from the source object.
     *
     * @param targetName target property name
     * @param provider   receives the source object
     *
     * @return this builder
     */
    public TypeMappingBuilder<S, T> provider(String targetName, ValueProvider<S> provider) {
        return property(targetName, builder -> builder.provider(provider));
    }

    /**
     * Fills a target property from the source object and the mapping context.
     *
     * @param targetName target property name
     * @param function   receives the source object and the context
     *
     * @return this builder
     */
    public TypeMappingBuilder<S, T> compute(String targetName, ComputeFunction<?> function) {
        return property(targetName, builder -> builder.compute(function));
    }

    /**
     * Tries several mappings in order and takes the first that yields a value.
     *
     * <p>The same-named source property is tried first, then each candidate.</p>
     *
     * @param targetName target property name
     * @param candidates fallbacks, in order
     *
     * @return this builder
     */
    public TypeMappingBuilder<S, T> coalesce(String targetName, PropertyMapping... candidates) {
        return property(targetName, builder -> builder.coalesce(candidates));
    }

    /**
     * Fills a target property only while the condition holds.
     *
     * @param targetName target property name
     * @param condition  evaluated against the source object and the context
     *
     * @return this builder
     */
    public TypeMappingBuilder<S, T> when(String targetName, BiPredicate<Object, MappingContext> condition) {
        return property(targetName, builder -> builder.when(condition));
    }

    /**
     * Fails the mapping when a target property resolves to nothing.
     *
     * @param targetName target property name
     * @param code       error code, matched by the errors policy
     * @param message    human-readable message
     *
     * @return this builder
     */
    public TypeMappingBuilder<S, T> required(String targetName, String code, String message) {
        return property(targetName, builder -> builder.required(code, message));
    }

    /**
     * Transforms whatever a target property resolved to.
     *
     * @param targetName  target property name
     * @param transformer applied to the resolved value
     *
     * @return this builder
     */
    public TypeMappingBuilder<S, T> transformer(String targetName, ValueTransformer transformer) {
        return property(targetName, builder -> builder.transform(transformer));
    }

    /**
     * Supplies a value for a target property that resolved to {@code null}.
     *
     * @param targetName   target property name
     * @param defaultValue value to fall back to
     *
     * @return this builder
     */
    public TypeMappingBuilder<S, T> defaultValue(String targetName, Object defaultValue) {
        return defaultValue(targetName, () -> defaultValue);
    }

    /**
     * Supplies a value lazily for a target property that resolved to {@code null}.
     *
     * @param targetName   target property name
     * @param defaultValue supplier of the fallback value
     *
     * @return this builder
     */
    public TypeMappingBuilder<S, T> defaultValue(String targetName, Supplier<?> defaultValue) {
        return property(targetName, builder -> builder.defaultValue(defaultValue));
    }

    /**
     * Builds an immutable {@link TypeMappingRule} from collected bindings.
     *
     * @return type mapping rule
     */
    public TypeMappingRule build() {
        return new TypeMappingRule(sourceType, targetType, bindings);
    }
}
