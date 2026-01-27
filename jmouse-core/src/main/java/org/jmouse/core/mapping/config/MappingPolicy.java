package org.jmouse.core.mapping.config;

/**
 * Mapping policy bundle.
 * Immutable and safe to share across threads.
 */
public final class MappingPolicy {

    private final UnassignedTargetPropertyPolicy unassignedTargetPropertyPolicy;
    private final NullHandlingPolicy             nullHandlingPolicy;
    private final TypeMismatchPolicy             typeMismatchPolicy;
    private final CollectionMappingPolicy        collectionMappingPolicy;
    private final ErrorHandlingPolicy            errorHandlingPolicy;

    private MappingPolicy(Builder builder) {
        this.unassignedTargetPropertyPolicy = builder.unassignedTargetPropertyPolicy;
        this.nullHandlingPolicy = builder.nullHandlingPolicy;
        this.typeMismatchPolicy = builder.typeMismatchPolicy;
        this.collectionMappingPolicy = builder.collectionMappingPolicy;
        this.errorHandlingPolicy = builder.errorHandlingPolicy;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static MappingPolicy defaults() {
        return builder().build();
    }

    public UnassignedTargetPropertyPolicy unassignedTargetPropertyPolicy() {
        return unassignedTargetPropertyPolicy;
    }

    public NullHandlingPolicy nullHandlingPolicy() {
        return nullHandlingPolicy;
    }

    public TypeMismatchPolicy typeMismatchPolicy() {
        return typeMismatchPolicy;
    }

    public CollectionMappingPolicy collectionMappingPolicy() {
        return collectionMappingPolicy;
    }

    public ErrorHandlingPolicy errorHandlingPolicy() {
        return errorHandlingPolicy;
    }

    public static final class Builder {

        private UnassignedTargetPropertyPolicy unassignedTargetPropertyPolicy = UnassignedTargetPropertyPolicy.LEAVE_DEFAULT;
        private NullHandlingPolicy nullHandlingPolicy = NullHandlingPolicy.PROPAGATE;
        private TypeMismatchPolicy typeMismatchPolicy = TypeMismatchPolicy.CONVERT_IF_POSSIBLE;
        private CollectionMappingPolicy collectionMappingPolicy = CollectionMappingPolicy.REPLACE;
        private ErrorHandlingPolicy errorHandlingPolicy = ErrorHandlingPolicy.FAIL_FAST;

        public Builder unassignedTargetPropertyPolicy(UnassignedTargetPropertyPolicy value) {
            this.unassignedTargetPropertyPolicy = value;
            return this;
        }

        public Builder nullHandlingPolicy(NullHandlingPolicy value) {
            this.nullHandlingPolicy = value;
            return this;
        }

        public Builder typeMismatchPolicy(TypeMismatchPolicy value) {
            this.typeMismatchPolicy = value;
            return this;
        }

        public Builder collectionMappingPolicy(CollectionMappingPolicy value) {
            this.collectionMappingPolicy = value;
            return this;
        }

        public Builder errorHandlingPolicy(ErrorHandlingPolicy value) {
            this.errorHandlingPolicy = value;
            return this;
        }

        public MappingPolicy build() {
            return new MappingPolicy(this);
        }
    }
}
