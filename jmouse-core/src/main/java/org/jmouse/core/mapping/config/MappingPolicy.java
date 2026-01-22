package org.jmouse.core.mapping.config;

/**
 * Mapping policy bundle.
 * Immutable and safe to share across threads.
 */
public final class MappingPolicy {

    private final UnknownSourcePropertyPolicy    unknownSourcePropertyPolicy;
    private final UnassignedTargetPropertyPolicy unassignedTargetPropertyPolicy;
    private final NullHandlingPolicy             nullHandlingPolicy;
    private final TypeMismatchPolicy             typeMismatchPolicy;
    private final CollectionMappingPolicy        collectionMappingPolicy;
    private final FieldNameMatchingPolicy        fieldNameMatchingPolicy;
    private final AmbiguityPolicy                ambiguityPolicy;
    private final ErrorHandlingPolicy            errorHandlingPolicy;
    private final VirtualFieldPolicy             virtualFieldPolicy;

    private MappingPolicy(Builder builder) {
        this.unknownSourcePropertyPolicy = builder.unknownSourcePropertyPolicy;
        this.unassignedTargetPropertyPolicy = builder.unassignedTargetPropertyPolicy;
        this.nullHandlingPolicy = builder.nullHandlingPolicy;
        this.typeMismatchPolicy = builder.typeMismatchPolicy;
        this.collectionMappingPolicy = builder.collectionMappingPolicy;
        this.fieldNameMatchingPolicy = builder.fieldNameMatchingPolicy;
        this.ambiguityPolicy = builder.ambiguityPolicy;
        this.errorHandlingPolicy = builder.errorHandlingPolicy;
        this.virtualFieldPolicy = builder.virtualFieldPolicy;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static MappingPolicy defaults() {
        return builder().build();
    }

    public UnknownSourcePropertyPolicy unknownSourcePropertyPolicy() {
        return unknownSourcePropertyPolicy;
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

    public FieldNameMatchingPolicy fieldNameMatchingPolicy() {
        return fieldNameMatchingPolicy;
    }

    public AmbiguityPolicy ambiguityPolicy() {
        return ambiguityPolicy;
    }

    public ErrorHandlingPolicy errorHandlingPolicy() {
        return errorHandlingPolicy;
    }

    public VirtualFieldPolicy virtualFieldPolicy() {
        return virtualFieldPolicy;
    }

    public static final class Builder {

        private UnknownSourcePropertyPolicy unknownSourcePropertyPolicy = UnknownSourcePropertyPolicy.IGNORE;
        private UnassignedTargetPropertyPolicy unassignedTargetPropertyPolicy = UnassignedTargetPropertyPolicy.LEAVE_DEFAULT;
        private NullHandlingPolicy nullHandlingPolicy = NullHandlingPolicy.PROPAGATE;
        private TypeMismatchPolicy typeMismatchPolicy = TypeMismatchPolicy.CONVERT_IF_POSSIBLE;
        private CollectionMappingPolicy collectionMappingPolicy = CollectionMappingPolicy.REPLACE;
        private FieldNameMatchingPolicy fieldNameMatchingPolicy = FieldNameMatchingPolicy.EXACT;
        private AmbiguityPolicy ambiguityPolicy = AmbiguityPolicy.FAIL;
        private ErrorHandlingPolicy errorHandlingPolicy = ErrorHandlingPolicy.FAIL_FAST;
        private VirtualFieldPolicy virtualFieldPolicy = VirtualFieldPolicy.USE_VIRTUAL_IF_SOURCE_MISSING;

        public Builder unknownSourcePropertyPolicy(UnknownSourcePropertyPolicy value) {
            this.unknownSourcePropertyPolicy = value;
            return this;
        }

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

        public Builder fieldNameMatchingPolicy(FieldNameMatchingPolicy value) {
            this.fieldNameMatchingPolicy = value;
            return this;
        }

        public Builder ambiguityPolicy(AmbiguityPolicy value) {
            this.ambiguityPolicy = value;
            return this;
        }

        public Builder errorHandlingPolicy(ErrorHandlingPolicy value) {
            this.errorHandlingPolicy = value;
            return this;
        }

        public Builder virtualFieldPolicy(VirtualFieldPolicy value) {
            this.virtualFieldPolicy = value;
            return this;
        }

        public MappingPolicy build() {
            return new MappingPolicy(this);
        }
    }
}
