package org.jmouse.core.mapping.config;

import org.jmouse.core.Verify;
import org.jmouse.core.mapping.errors.ErrorsPolicy;
import org.jmouse.core.mapping.plugin.MappingPlugin;

import java.util.*;
import java.util.function.Supplier;

/**
 * Runtime configuration for the mapping engine. ⚙️
 *
 * <p>{@code MappingConfig} groups tunable options that affect how values are materialized and how
 * failures are handled during mapping.</p>
 *
 * <h3>Factories</h3>
 * <p>Collection/map factories control which concrete implementations are used when the engine needs
 * to create new targets (e.g., mapping into {@link List}, {@link Set}, {@link Map}).</p>
 *
 * <h3>Limits</h3>
 * <p>{@link #maxCollectionSize()} is a safety guard to prevent unbounded allocations when mapping
 * very large or accidentally infinite iterables.</p>
 *
 * <h3>Object-to-map</h3>
 * <p>{@link #mapKeyPolicy()} controls how non-string keys are treated when mapping into
 * {@code Map<String, ?>}. {@link #objectToMapMode()} controls the materialization strategy
 * (e.g., tree-style expansion).</p>
 *
 * <h3>Errors & Plugins</h3>
 * <p>{@link #errorsPolicy()} defines how mapping exceptions are handled (throw/warn/silent).
 * {@link #plugins()} provides mapping plugins executed by the root invocation.</p>
 *
 * @see ErrorsPolicy
 * @see MappingPlugin
 */
public final class MappingConfig {

    private final Supplier<? extends List<Object>>        listFactory;
    private final Supplier<? extends Set<Object>>         setFactory;
    private final Supplier<? extends Collection<Object>>  collectionFactory; // fallback
    private final Supplier<? extends Map<Object, Object>> mapFactory;        // fallback

    private final ArrayMaterializationPolicy arrayMaterializationPolicy;
    private final int                        maxCollectionSize;

    private final MapKeyPolicy    mapKeyPolicy;
    private final ObjectToMapMode objectToMapMode;

    private final ErrorsPolicy errorsPolicy;

    private final List<MappingPlugin> plugins;

    private MappingConfig(Builder builder) {
        this.arrayMaterializationPolicy = builder.arrayMaterializationPolicy;
        this.listFactory                = builder.listFactory;
        this.setFactory                 = builder.setFactory;
        this.collectionFactory          = builder.collectionFactory;
        this.mapFactory                 = builder.mapFactory;
        this.maxCollectionSize          = builder.maxCollectionSize;
        this.mapKeyPolicy               = builder.mapKeyPolicy;
        this.objectToMapMode            = builder.objectToMapMode;
        this.plugins                    = builder.plugins;
        this.errorsPolicy               = builder.errorsPolicy;
    }

    /**
     * Create a config instance using defaults defined by {@link Builder}.
     *
     * @return default mapping configuration
     */
    public static MappingConfig defaults() {
        return builder().build();
    }

    /**
     * Create a new config builder.
     *
     * @return builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Return the error handling policy.
     *
     * @return error policy
     */
    public ErrorsPolicy errorsPolicy() {
        return errorsPolicy;
    }

    /**
     * Return the policy used to coerce non-string map keys when mapping into {@code Map<String, ?>}.
     *
     * @return map key policy
     */
    public MapKeyPolicy mapKeyPolicy() {
        return mapKeyPolicy;
    }

    /**
     * Return the object-to-map materialization mode.
     *
     * @return object-to-map mode
     */
    public ObjectToMapMode objectToMapMode() {
        return objectToMapMode;
    }

    /**
     * Return mapping plugins enabled for the root mapping invocation.
     *
     * @return plugin list (may be empty)
     */
    public List<MappingPlugin> plugins() {
        return plugins;
    }

    /**
     * Policy controlling how arrays are materialized during mapping.
     *
     * @return array materialization policy
     */
    public ArrayMaterializationPolicy arrayMaterializationPolicy() {
        return arrayMaterializationPolicy;
    }

    /**
     * Factory used to create target {@link List} instances.
     *
     * @return list factory
     */
    public Supplier<? extends List<Object>> listFactory() {
        return listFactory;
    }

    /**
     * Factory used to create target {@link Set} instances.
     *
     * @return set factory
     */
    public Supplier<? extends Set<Object>> setFactory() {
        return setFactory;
    }

    /**
     * Fallback factory used to create generic {@link Collection} instances.
     *
     * @return collection factory
     */
    public Supplier<? extends Collection<Object>> collectionFactory() {
        return collectionFactory;
    }

    /**
     * Fallback factory used to create target {@link Map} instances.
     *
     * @return map factory
     */
    public Supplier<? extends Map<Object, Object>> mapFactory() {
        return mapFactory;
    }

    /**
     * Maximum number of elements allowed when materializing collections/iterables.
     *
     * @return maximum collection size
     */
    public int maxCollectionSize() {
        return maxCollectionSize;
    }

    /**
     * Builder for {@link MappingConfig}. 🧱
     */
    public static final class Builder {

        private Supplier<? extends List<Object>>        listFactory       = ArrayList::new;
        private Supplier<? extends Set<Object>>         setFactory        = LinkedHashSet::new;
        private Supplier<? extends Collection<Object>>  collectionFactory = ArrayList::new;
        private Supplier<? extends Map<Object, Object>> mapFactory        = LinkedHashMap::new;

        private ArrayMaterializationPolicy arrayMaterializationPolicy =
                ArrayMaterializationPolicy.STREAM_WHEN_SIZED;

        private MapKeyPolicy    mapKeyPolicy    = MapKeyPolicy.STRINGIFY;
        private ObjectToMapMode objectToMapMode = ObjectToMapMode.TREE;

        private int maxCollectionSize = 10_000;

        private List<MappingPlugin> plugins = List.of();

        private ErrorsPolicy errorsPolicy = ErrorsPolicy.builder().build();

        /**
         * Configure the factory used to create {@link List} instances.
         *
         * @param factory list factory
         * @return this builder
         */
        public Builder listFactory(Supplier<? extends List<Object>> factory) {
            this.listFactory = Verify.nonNull(factory, "listFactory");
            return this;
        }

        /**
         * Configure the factory used to create {@link Set} instances.
         *
         * @param factory set factory
         * @return this builder
         */
        public Builder setFactory(Supplier<? extends Set<Object>> factory) {
            this.setFactory = Verify.nonNull(factory, "setFactory");
            return this;
        }

        /**
         * Configure the fallback factory used to create generic {@link Collection} instances.
         *
         * @param factory collection factory
         * @return this builder
         */
        public Builder collectionFactory(Supplier<? extends Collection<Object>> factory) {
            this.collectionFactory = Verify.nonNull(factory, "collectionFactory");
            return this;
        }

        /**
         * Configure the fallback factory used to create {@link Map} instances.
         *
         * @param factory map factory
         * @return this builder
         */
        public Builder mapFactory(Supplier<? extends Map<Object, Object>> factory) {
            this.mapFactory = Verify.nonNull(factory, "mapFactory");
            return this;
        }

        /**
         * Configure array materialization behavior.
         *
         * @param v array materialization policy
         * @return this builder
         */
        public Builder arrayMaterializationPolicy(ArrayMaterializationPolicy v) {
            this.arrayMaterializationPolicy = Verify.nonNull(v, "arrayMaterializationPolicy");
            return this;
        }

        /**
         * Configure maximum allowed collection size during materialization.
         *
         * @param value max size; must be {@code > 0}
         * @return this builder
         */
        public Builder maxCollectionSize(int value) {
            Verify.state(value > 0, "maxCollectionSize must be > 0");
            this.maxCollectionSize = value;
            return this;
        }

        /**
         * Configure key coercion policy when mapping into {@code Map<String, ?>}.
         *
         * @param value map key policy
         * @return this builder
         */
        public Builder mapKeyPolicy(MapKeyPolicy value) {
            this.mapKeyPolicy = Verify.nonNull(value, "mapKeyPolicy");
            return this;
        }

        /**
         * Configure object-to-map materialization mode.
         *
         * @param value object-to-map mode
         * @return this builder
         */
        public Builder objectToMapMode(ObjectToMapMode value) {
            this.objectToMapMode = Verify.nonNull(value, "objectToMapMode");
            return this;
        }

        /**
         * Configure mapping plugins.
         *
         * <p>If not configured, defaults to an empty list.</p>
         *
         * @param plugins plugin list
         * @return this builder
         */
        public Builder plugins(List<MappingPlugin> plugins) {
            this.plugins = Verify.nonNull(plugins, "plugins");
            return this;
        }

        /**
         * Configure error handling policy.
         *
         * @param policy errors policy
         * @return this builder
         */
        public Builder errorsPolicy(ErrorsPolicy policy) {
            this.errorsPolicy = Verify.nonNull(policy, "errorsPolicy");
            return this;
        }

        /**
         * Build the immutable {@link MappingConfig}.
         *
         * @return config instance
         */
        public MappingConfig build() {
            return new MappingConfig(this);
        }
    }
}
