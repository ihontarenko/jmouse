package org.jmouse.parallax;

import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.DefinitionCompiler;
import org.jmouse.parallax.object.Interval;
import org.jmouse.parallax.object.ObjectDefinition;

import java.util.List;

/** Shared sampling helpers and the settings several computations declare alike. */
public final class Sampling {

    public static final SettingSpecification RESOLUTION = SettingSpecification.integer("resolution", 96, 8, 512,
            "Samples per axis of the parameter grid");
    public static final SettingSpecification FIELD_RESOLUTION = SettingSpecification.integer("resolution", 16, 4, 64,
            "Samples per axis of the field");
    public static final SettingSpecification CURVE_SAMPLES = SettingSpecification.integer("samples", 512, 16, 20_000,
            "Points along the curve");

    private Sampling() {
    }

    public static float[] grid(ScalarAt function, Interval x, Interval y, int columns, int rows,
                               ComputationProgress progress) {
        float[] values = new float[columns * rows];
        for (int row = 0; row < rows; row++) {
            if (row % 32 == 0) {
                progress.checkpoint((double) row / rows, "sampling");
            }
            double yValue = y.sample(row, rows);
            for (int column = 0; column < columns; column++) {
                values[row * columns + column] = (float) function.at(x.sample(column, columns), yValue);
            }
        }
        return values;
    }

    @FunctionalInterface
    public interface ScalarAt {
        double at(double x, double y);
    }

    public static double[] range(float[] values) {
        double minimum = Double.POSITIVE_INFINITY;
        double maximum = Double.NEGATIVE_INFINITY;
        for (float value : values) {
            if (Float.isFinite(value)) {
                minimum = Math.min(minimum, value);
                maximum = Math.max(maximum, value);
            }
        }
        if (minimum > maximum) {
            return new double[]{0, 0};
        }
        return new double[]{minimum, maximum};
    }

    /** The plane domain of a 2D-parameterized object: an explicit surface's x×y, a field's first two axes. */
    public static Interval[] planeDomain(ObjectDefinition definition) {
        return switch (definition) {
            case ObjectDefinition.ExplicitSurface surface -> new Interval[]{surface.x(), surface.y()};
            case ObjectDefinition.ImplicitCurve curve -> new Interval[]{curve.x(), curve.y()};
            case ObjectDefinition.VectorField field -> new Interval[]{field.bounds().get(0), field.bounds().get(1)};
            case ObjectDefinition.ComplexMapping mapping -> new Interval[]{mapping.real(), mapping.imaginary()};
            case ObjectDefinition.ProbabilityDistribution distribution when distribution.support().size() == 2 ->
                    new Interval[]{distribution.support().get(0), distribution.support().get(1)};
            case ObjectDefinition.NonlinearTransformation transformation ->
                    new Interval[]{transformation.bounds().get(0), transformation.bounds().get(1)};
            default -> throw new IllegalArgumentException("This object has no plane domain.");
        };
    }

    public static long square(int resolution) {
        return (long) resolution * resolution;
    }

    public static List<String> variables(CompiledObject object) {
        return DefinitionCompiler.variablesOf(object.definition());
    }
}
