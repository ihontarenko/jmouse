package org.jmouse.core.parameters;

/**
 * ⚙️ How a tree becomes plain Java ({@link RequestParametersJavaStructureConverter}).
 *
 * @param unwrapSingleScalarValue  a name with one value is that value, not a list of one
 * @param unwrapSingleElementArray a list of one item is that item
 * @param dropNullArrayItems       the places a sparse list leaves empty are left out, so its items follow one another in their order
 */
public record RequestParametersJavaStructureOptions(
        boolean unwrapSingleScalarValue,
        boolean unwrapSingleElementArray,
        boolean dropNullArrayItems
) {
    public static RequestParametersJavaStructureOptions defaults() {
        return new RequestParametersJavaStructureOptions(true, false, true);
    }
}
