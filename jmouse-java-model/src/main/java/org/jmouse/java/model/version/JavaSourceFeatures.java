package org.jmouse.java.model.version;



/** Stable public identities shared by the source parser, analysis and implementation coverage manifests. */
public final class JavaSourceFeatures {
    public static final JavaFeatureId UNDERSCORE_KEYWORD =
            new JavaFeatureId("java.source.underscore-keyword");
    public static final JavaFeatureId MODULES =
            new JavaFeatureId("java.source.modules");
    public static final JavaFeatureId LOCAL_VARIABLE_INFERENCE =
            new JavaFeatureId("java.source.local-variable-inference");
    public static final JavaFeatureId LAMBDA_VAR_PARAMETERS =
            new JavaFeatureId("java.source.lambda-var-parameters");
    public static final JavaFeatureId SWITCH_EXPRESSIONS =
            new JavaFeatureId("java.source.switch-expressions");
    public static final JavaFeatureId TEXT_BLOCKS =
            new JavaFeatureId("java.source.text-blocks");
    public static final JavaFeatureId RECORD_DECLARATIONS =
            new JavaFeatureId("java.source.record-declarations");
    public static final JavaFeatureId INSTANCEOF_PATTERNS =
            new JavaFeatureId("java.source.instanceof-patterns");
    public static final JavaFeatureId SEALED_TYPES =
            new JavaFeatureId("java.source.sealed-types");
    public static final JavaFeatureId SWITCH_PATTERNS =
            new JavaFeatureId("java.source.switch-patterns");
    public static final JavaFeatureId RECORD_PATTERNS =
            new JavaFeatureId("java.source.record-patterns");

    private JavaSourceFeatures() {
    }
}
