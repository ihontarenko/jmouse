package org.jmouse.core.mapping.binding;

import org.jmouse.core.Verify;
import org.jmouse.core.mapping.errors.MappingException;

/**
 * Describes how a single target property should be populated during mapping. 🧩
 *
 * <p>{@code PropertyBinding} is a small immutable strategy object used by the mapping engine
 * to decide where the value for {@link #targetName()} comes from.</p>
 *
 * <h2>Examples</h2>
 * <pre>{@code
 * // Ignore target property (leave default / do not touch)
 * PropertyBinding b1 = PropertyBinding.ignore("password");
 *
 * // Assign constant value
 * PropertyBinding b2 = PropertyBinding.constant("role", "USER");
 *
 * // Read from source path/reference
 * PropertyBinding b3 = PropertyBinding.reference("username", "details.username");
 *
 * // Compute from typed source
 * PropertyBinding b4 = PropertyBinding.compute(
 *     "fullName",
 *     User.class,
 *     (user, ctx) -> user.firstName() + " " + user.lastName()
 * );
 *
 * // Provide value from typed source (no MappingContext usage)
 * PropertyBinding b5 = PropertyBinding.valueProvider(
 *     "isAdult",
 *     User.class,
 *     user -> user.age() >= 18
 * );
 * }</pre>
 *
 * <h3>Notes</h3>
 * <ul>
 *   <li>{@link Provider} and {@link Compute} may be defined as typed functions, but are stored internally
 *       as {@code Object}-based handlers with runtime type checks.</li>
 *   <li>If a typed binding receives an incompatible source object, a {@link MappingException} is thrown.</li>
 * </ul>
 *
 * @see ValueProvider
 * @see ComputeFunction
 */
public sealed interface PropertyMapping
        permits PropertyMapping.Ignore,
                PropertyMapping.Constant,
                PropertyMapping.Reference,
                PropertyMapping.Provider,
                PropertyMapping.Compute {

    /**
     * Target property name this binding is responsible for.
     *
     * @return target property name (never {@code null})
     */
    String targetName();

    /**
     * Binding that marks a target property as ignored.
     *
     * <p>Ignored properties are not written by the mapperProvider.
     * Typically used to skip sensitive or derived fields.</p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * PropertyBinding.Ignore binding = PropertyBinding.ignore("passwordHash");
     * }</pre>
     *
     * @param targetName target property name
     */
    record Ignore(String targetName) implements PropertyMapping {
        public Ignore {
            Verify.nonNull(targetName, "targetName");
        }
    }

    /**
     * Binding that assigns a constant value to a target property.
     *
     * <h3>Example</h3>
     * <pre>{@code
     * PropertyBinding.Constant binding =
     *     PropertyBinding.constant("status", "ACTIVE");
     * }</pre>
     *
     * @param targetName target property name
     * @param value constant value (may be {@code null})
     */
    record Constant(String targetName, Object value) implements PropertyMapping {
        public Constant {
            Verify.nonNull(targetName, "targetName");
        }
    }

    /**
     * Binding that reads a value from the source object via a source reference/path.
     *
     * <p>The exact syntax of {@code sourceReference} depends on the active source accessor
     * (e.g., bean property accessor, map accessor, properties-flattened accessor).</p>
     *
     * <h3>Examples</h3>
     * <pre>{@code
     * // bean-like property
     * PropertyBinding.Reference a = PropertyBinding.reference("username", "username");
     *
     * // nested property
     * PropertyBinding.Reference b = PropertyBinding.reference("status", "details.status");
     *
     * // bracket syntax (if supported by accessor)
     * PropertyBinding.Reference c = PropertyBinding.reference("userName", "[user_name]");
     * }</pre>
     *
     * @param targetName target property name
     * @param sourceReference source reference/path used by the accessor
     */
    record Reference(String targetName, String sourceReference) implements PropertyMapping {
        public Reference {
            Verify.nonNull(targetName, "targetName");
            Verify.nonNull(sourceReference, "sourceReference");
        }
    }

    /**
     * Binding that provides a value using a custom valueProvider function.
     *
     * <p>This is useful for "pulling" values from the source object when you don't want to rely
     * on reflective property access or path resolution.</p>
     *
     * <p>Typed providers are adapted into an {@code Object}-based valueProvider with a runtime
     * source type check (see {@link #typed(String, Class, ValueProvider)}).</p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * PropertyBinding.Provider b = PropertyBinding.valueProvider(
     *     "ageGroup",
     *     User.class,
     *     u -> u.age() >= 18 ? "adult" : "child"
     * );
     * }</pre>
     *
     * @param targetName target property name
     * @param valueProvider valueProvider function (never {@code null})
     */
    record Provider(String targetName, ValueProvider<Object> valueProvider) implements PropertyMapping {
        public Provider {
            Verify.nonNull(targetName, "targetName");
            Verify.nonNull(valueProvider, "valueProvider");
        }

        /**
         * Create a type-safe valueProvider binding that validates the runtime source type.
         *
         * <p>If {@code source} is non-null but not an instance of {@code sourceType},
         * a {@link MappingException} is thrown.</p>
         *
         * <h3>Example</h3>
         * <pre>{@code
         * PropertyBinding.Provider b = PropertyBinding.Provider.typed(
         *     "fullName",
         *     User.class,
         *     u -> u.firstName() + " " + u.lastName()
         * );
         * }</pre>
         *
         * @param targetName target property name
         * @param sourceType expected runtime type of source object
         * @param provider typed valueProvider
         * @param <S> source type
         * @return adapted valueProvider binding with runtime checks
         * @throws IllegalArgumentException if {@code sourceType} or {@code valueProvider} is {@code null}
         */
        public static <S> Provider typed(String targetName, Class<S> sourceType, ValueProvider<? super S> provider) {
            Verify.nonNull(sourceType, "sourceType");
            Verify.nonNull(provider, "valueProvider");

            ValueProvider<Object> adapted = source -> {
                if (source == null) {
                    return null;
                }

                if (!sourceType.isInstance(source)) {
                    throw new MappingException(
                            "binding_source_type_mismatch",
                            "Provider for '%s' expects %s but got %s".formatted(
                                    targetName, sourceType.getName(), source.getClass().getName()
                            ),
                            null
                    );
                }

                return provider.provide(sourceType.cast(source));
            };

            return new Provider(targetName, adapted);
        }
    }

    /**
     * Binding that computes a value using a custom compute function that also receives {@code MappingContext}.
     *
     * <p>This is the most powerful binding variant: it can use both the source object and mapping runtime
     * context to decide what value should be written.</p>
     *
     * <p>Typed compute functions are adapted into an {@code Object}-based function with a runtime
     * source type check (see {@link #typed(String, Class, ComputeFunction)}).</p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * PropertyBinding.Compute b = PropertyBinding.compute(
     *     "displayName",
     *     User.class,
     *     (u, ctx) -> u.nickname() != null ? u.nickname() : u.firstName()
     * );
     * }</pre>
     *
     * @param targetName target property name
     * @param function compute function (never {@code null})
     */
    record Compute(String targetName, ComputeFunction<Object> function) implements PropertyMapping {
        public Compute {
            Verify.nonNull(targetName, "targetName");
            Verify.nonNull(function, "function");
        }

        /**
         * Create a type-safe compute binding that validates the runtime source type.
         *
         * <p>If {@code source} is non-null but not an instance of {@code sourceType},
         * a {@link MappingException} is thrown.</p>
         *
         * <h3>Example</h3>
         * <pre>{@code
         * PropertyBinding.Compute b = PropertyBinding.Compute.typed(
         *     "total",
         *     Order.class,
         *     (o, ctx) -> o.items().stream().mapToLong(Item::price).sum()
         * );
         * }</pre>
         *
         * @param targetName target property name
         * @param sourceType expected runtime type of source object
         * @param function typed compute function
         * @param <S> source type
         * @return adapted compute binding with runtime checks
         * @throws IllegalArgumentException if {@code sourceType} or {@code function} is {@code null}
         */
        public static <S> Compute typed(String targetName, Class<S> sourceType, ComputeFunction<? super S> function) {
            Verify.nonNull(sourceType, "sourceType");
            Verify.nonNull(function, "function");

            ComputeFunction<Object> adapted = (source, context) -> {
                if (source == null) {
                    return null;
                }
                if (!sourceType.isInstance(source)) {
                    throw new MappingException(
                            "compute_source_type_mismatch",
                            "Compute for '%s' expects %s but got %s".formatted(
                                    targetName, sourceType.getName(), source.getClass().getName()
                            ),
                            null
                    );
                }
                return function.compute(sourceType.cast(source), context);
            };

            return new Compute(targetName, adapted);
        }
    }

    /**
     * Create an {@link Ignore} binding.
     *
     * @param targetName target property name
     * @return ignore binding
     */
    static Ignore ignore(String targetName) {
        return new Ignore(targetName);
    }

    /**
     * Create a {@link Constant} binding.
     *
     * @param targetName target property name
     * @param value constant value (may be {@code null})
     * @return constant binding
     */
    static Constant constant(String targetName, Object value) {
        return new Constant(targetName, value);
    }

    /**
     * Create a {@link Reference} binding.
     *
     * @param targetName target property name
     * @param sourceReference source reference/path
     * @return reference binding
     */
    static Reference reference(String targetName, String sourceReference) {
        return new Reference(targetName, sourceReference);
    }

    /**
     * Create a typed {@link Provider} binding.
     *
     * @param targetName target property name
     * @param sourceType expected runtime type of source object
     * @param provider typed valueProvider
     * @param <S> source type
     * @return valueProvider binding
     */
    static <S> Provider provider(String targetName, Class<S> sourceType, ValueProvider<? super S> provider) {
        return Provider.typed(targetName, sourceType, provider);
    }

    /**
     * Create a typed {@link Compute} binding.
     *
     * @param targetName target property name
     * @param sourceType expected runtime type of source object
     * @param function typed compute function
     * @param <S> source type
     * @return compute binding
     */
    static <S> Compute compute(String targetName, Class<S> sourceType, ComputeFunction<? super S> function) {
        return Compute.typed(targetName, sourceType, function);
    }

}
