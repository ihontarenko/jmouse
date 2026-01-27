package org.jmouse.core.mapping.config;

public final class MappingPolicy {

    private final NullHandlingPolicy             nullHandlingPolicy;
    private final TypeMismatchPolicy             typeMismatchPolicy;
    private final UnassignedTargetPropertyPolicy unassignedTargetPropertyPolicy;
    private final CollectionMappingPolicy        collectionMappingPolicy;

    private MappingPolicy(Builder builder) {
        this.nullHandlingPolicy = builder.nullHandlingPolicy;
        this.typeMismatchPolicy = builder.typeMismatchPolicy;
        this.unassignedTargetPropertyPolicy = builder.unassignedTargetPropertyPolicy;
        this.collectionMappingPolicy = builder.collectionMappingPolicy;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static MappingPolicy defaults() {
        return builder().build();
    }

    public NullHandlingPolicy nullHandlingPolicy() {
        return nullHandlingPolicy;
    }

    public TypeMismatchPolicy typeMismatchPolicy() {
        return typeMismatchPolicy;
    }

    public UnassignedTargetPropertyPolicy unassignedTargetPropertyPolicy() {
        return unassignedTargetPropertyPolicy;
    }

    public CollectionMappingPolicy collectionMappingPolicy() {
        return collectionMappingPolicy;
    }

    public static final class Builder {
        private NullHandlingPolicy             nullHandlingPolicy             = NullHandlingPolicy.PROPAGATE;
        private TypeMismatchPolicy             typeMismatchPolicy             = TypeMismatchPolicy.CONVERT_IF_POSSIBLE;
        private UnassignedTargetPropertyPolicy unassignedTargetPropertyPolicy = UnassignedTargetPropertyPolicy.LEAVE_DEFAULT;
        private CollectionMappingPolicy        collectionMappingPolicy        = CollectionMappingPolicy.REPLACE;

        public Builder nullHandlingPolicy(NullHandlingPolicy policy) {
            this.nullHandlingPolicy = policy;
            return this;
        }

        public Builder typeMismatchPolicy(TypeMismatchPolicy policy) {
            this.typeMismatchPolicy = policy;
            return this;
        }

        public Builder unassignedTargetPropertyPolicy(UnassignedTargetPropertyPolicy policy) {
            this.unassignedTargetPropertyPolicy = policy;
            return this;
        }

        public Builder collectionMappingPolicy(CollectionMappingPolicy policy) {
            this.collectionMappingPolicy = policy;
            return this;
        }

        public MappingPolicy build() {
            return new MappingPolicy(this);
        }
    }
}
