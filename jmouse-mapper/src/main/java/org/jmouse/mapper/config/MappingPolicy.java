package org.jmouse.mapper.config;

public final class MappingPolicy {

    private final NullHandlingPolicy             nullHandlingPolicy;
    private final TypeMismatchPolicy             typeMismatchPolicy;
    private final UnassignedTargetPropertyPolicy unassignedTargetPropertyPolicy;
    private final CollectionMappingPolicy        collectionMappingPolicy;
    private final ReferenceMappingPolicy         referenceMappingPolicy;

    private MappingPolicy(Builder builder) {
        this.nullHandlingPolicy = builder.nullHandlingPolicy;
        this.typeMismatchPolicy = builder.typeMismatchPolicy;
        this.unassignedTargetPropertyPolicy = builder.unassignedTargetPropertyPolicy;
        this.collectionMappingPolicy = builder.collectionMappingPolicy;
        this.referenceMappingPolicy = builder.referenceMappingPolicy;
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

    public ReferenceMappingPolicy referenceMappingPolicy() {
        return referenceMappingPolicy;
    }

    public static final class Builder {
        /**
         * ⚠️ {@link NullHandlingPolicy#SKIP} rather than {@code PROPAGATE}: until this policy was
         * executed at all, the engine skipped every null unconditionally. Shipping {@code PROPAGATE}
         * as the default would start clearing target properties in every existing caller as a side
         * effect of the policy beginning to work. Ask for {@code PROPAGATE} to get that behaviour.
         */
        private NullHandlingPolicy             nullHandlingPolicy             = NullHandlingPolicy.SKIP;
        private TypeMismatchPolicy             typeMismatchPolicy             = TypeMismatchPolicy.CONVERT_IF_POSSIBLE;
        private UnassignedTargetPropertyPolicy unassignedTargetPropertyPolicy = UnassignedTargetPropertyPolicy.LEAVE_DEFAULT;
        private CollectionMappingPolicy        collectionMappingPolicy        = CollectionMappingPolicy.REPLACE;
        private ReferenceMappingPolicy         referenceMappingPolicy         = ReferenceMappingPolicy.BREAK;

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

        public Builder referenceMappingPolicy(ReferenceMappingPolicy policy) {
            this.referenceMappingPolicy = policy;
            return this;
        }

        public MappingPolicy build() {
            return new MappingPolicy(this);
        }
    }
}
