package org.jmouse.core.mapping.config;

import org.jmouse.core.Verify;
import org.jmouse.core.mapping.plugin.MappingPlugin;

import java.util.*;
import java.util.function.Supplier;

public final class MappingConfig {

    private final Supplier<? extends List<Object>>        listFactory;
    private final Supplier<? extends Set<Object>>         setFactory;
    private final Supplier<? extends Collection<Object>>  collectionFactory; // fallback
    private final Supplier<? extends Map<Object, Object>> mapFactory;        // fallback

    private final ArrayMaterializationPolicy arrayMaterializationPolicy;
    private final int                        maxCollectionSize;

    private final List<MappingPlugin> plugins;

    private MappingConfig(Builder builder) {
        this.arrayMaterializationPolicy = builder.arrayMaterializationPolicy;
        this.listFactory                = builder.listFactory;
        this.setFactory                 = builder.setFactory;
        this.collectionFactory          = builder.collectionFactory;
        this.mapFactory                 = builder.mapFactory;
        this.maxCollectionSize          = builder.maxCollectionSize;
        this.plugins                    = builder.plugins;
    }

    public static MappingConfig defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<MappingPlugin> plugins() {
        return plugins;
    }

    public ArrayMaterializationPolicy arrayMaterializationPolicy() {
        return arrayMaterializationPolicy;
    }

    public Supplier<? extends List<Object>> listFactory() {
        return listFactory;
    }

    public Supplier<? extends Set<Object>> setFactory() {
        return setFactory;
    }

    public Supplier<? extends Collection<Object>> collectionFactory() {
        return collectionFactory;
    }

    public Supplier<? extends Map<Object, Object>> mapFactory() {
        return mapFactory;
    }

    public int maxCollectionSize() {
        return maxCollectionSize;
    }

    public static final class Builder {

        private Supplier<? extends List<Object>>        listFactory       = ArrayList::new;
        private Supplier<? extends Set<Object>>         setFactory        = LinkedHashSet::new;
        private Supplier<? extends Collection<Object>>  collectionFactory = ArrayList::new;
        private Supplier<? extends Map<Object, Object>> mapFactory        = LinkedHashMap::new;

        private ArrayMaterializationPolicy arrayMaterializationPolicy =
                ArrayMaterializationPolicy.STREAM_WHEN_SIZED;

        private int maxCollectionSize = 10_000;

        private List<MappingPlugin> plugins;

        public Builder listFactory(Supplier<? extends List<Object>> factory) {
            this.listFactory = Verify.nonNull(factory, "listFactory");
            return this;
        }

        public Builder setFactory(Supplier<? extends Set<Object>> factory) {
            this.setFactory = Verify.nonNull(factory, "setFactory");
            return this;
        }

        public Builder collectionFactory(Supplier<? extends Collection<Object>> factory) {
            this.collectionFactory = Verify.nonNull(factory, "collectionFactory");
            return this;
        }

        public Builder mapFactory(Supplier<? extends Map<Object, Object>> factory) {
            this.mapFactory = Verify.nonNull(factory, "mapFactory");
            return this;
        }

        public Builder arrayMaterializationPolicy(ArrayMaterializationPolicy v) {
            this.arrayMaterializationPolicy = Verify.nonNull(v, "arrayMaterializationPolicy");
            return this;
        }

        public Builder maxCollectionSize(int value) {
            Verify.state(value > 0, "maxCollectionSize must be > 0");
            this.maxCollectionSize = value;
            return this;
        }

        public Builder plugins(List<MappingPlugin> plugins) {
            this.plugins = Verify.nonNull(plugins, "plugins");
            return this;
        }

        public MappingConfig build() {
            return new MappingConfig(this);
        }
    }
}
