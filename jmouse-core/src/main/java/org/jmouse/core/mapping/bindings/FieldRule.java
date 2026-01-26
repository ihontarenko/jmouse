package org.jmouse.core.mapping.bindings;

public record FieldRule(
        String targetName,
        FieldRuleType type,
        String sourceReference,
        SourceValueProvider valueProvider,
        Object constantValue,
        FieldComputeFunction computeFunction
) {

    public static FieldRule bind(String targetName, String sourceReference) {
        return new FieldRule(targetName, FieldRuleType.BIND, sourceReference, null, null, null);
    }

    public static FieldRule bind(String targetName, SourceValueProvider provider) {
        return new FieldRule(targetName, FieldRuleType.BIND, null, provider, null, null);
    }

    public static FieldRule ignore(String targetName) {
        return new FieldRule(targetName, FieldRuleType.IGNORE, null, null, null, null);
    }

    public static FieldRule constant(String targetName, Object value) {
        return new FieldRule(targetName, FieldRuleType.CONSTANT, null, null, value, null);
    }

    public static FieldRule compute(String targetName, FieldComputeFunction function) {
        return new FieldRule(targetName, FieldRuleType.COMPUTE, null, null, null, function);
    }
}
