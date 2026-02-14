package org.jmouse.core.parameters;

public record RequestParametersJavaStructureOptions(
        boolean unwrapSingleScalarValue,
        boolean unwrapSingleElementArray,
        boolean dropNullArrayItems
) {
    public static RequestParametersJavaStructureOptions defaults() {
        return new RequestParametersJavaStructureOptions(
                true,   // "uk" instead of ["uk"]
                false,  // keep arrays as arrays by default
                true    // sparse arrays: remove nulls (optional)
        );
    }
}
