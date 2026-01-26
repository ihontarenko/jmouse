package org.jmouse.core.mapping.bindings;

import org.jmouse.core.Verify;
import org.jmouse.core.mapping.errors.MappingException;

public record FieldBinding(
        String targetName,
        BindingType type,
        String sourceReference,
        ValueProvider<Object> valueProvider,
        Object constantValue,
        ComputeFunction<Object> computeFunction
) {

    public static FieldBinding bind(String targetName, String sourceReference) {
        return new FieldBinding(targetName, BindingType.BIND, sourceReference, null, null, null);
    }

    public static <S> FieldBinding bind(String targetName, Class<S> sourceType, ValueProvider<? super S> provider) {
        Verify.nonNull(sourceType, "sourceType");
        Verify.nonNull(provider, "provider");

        ValueProvider<Object> adapted = source -> {
            if (source == null) {
                return null;
            }

            if (!sourceType.isInstance(source)) {
                throw new MappingException(
                        "binding_source_type_mismatch",
                        "Binding provider for '%s' expects %s but got %s".formatted(
                                targetName, sourceType.getName(), source.getClass().getName()
                        )
                );
            }

            return provider.provide(sourceType.cast(source));
        };

        return new FieldBinding(targetName, BindingType.BIND, null, adapted, null, null);
    }

    public static FieldBinding ignore(String targetName) {
        return new FieldBinding(targetName, BindingType.IGNORE, null, null, null, null);
    }

    public static FieldBinding constant(String targetName, Object value) {
        return new FieldBinding(targetName, BindingType.CONSTANT, null, null, value, null);
    }

    public static <S> FieldBinding compute(String targetName, Class<S> sourceType, ComputeFunction<? super S> function) {
        Verify.nonNull(sourceType, "sourceType");
        Verify.nonNull(function, "function");

        ComputeFunction<Object> adapted = (source, context) -> {
            if (source == null) {
                return null;
            }

            if (!sourceType.isInstance(source)) {
                throw new MappingException(
                        "compute_source_type_mismatch",
                        "Compute function for '%s' expects %s but got %s".formatted(
                                targetName, sourceType.getName(), source.getClass().getName()
                        )
                );
            }

            return function.compute(sourceType.cast(source), context);
        };

        return new FieldBinding(targetName, BindingType.COMPUTE, null, null, null, adapted);
    }
}
