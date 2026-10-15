package org.jmouse.parallax.analysis;

import org.jmouse.parallax.object.BoundFunction;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.Interval;
import org.jmouse.parallax.object.ObjectDefinition.ExplicitSurface;
import org.jmouse.parallax.object.ObjectDefinition.ParametricSurface;

import java.util.ArrayList;
import java.util.List;

/**
 * A surface in R³ as a map r(u, v) with its exact first and second partial derivatives: the moving frame normals
 * and curvature are read from. z = f(x, y) is the map (u, v) ↦ (u, v, f(u, v)).
 */
final class SurfaceFrame {

    private final Interval u;
    private final Interval v;
    private final List<BoundFunction> position;
    private final List<BoundFunction> du;
    private final List<BoundFunction> dv;
    private final List<BoundFunction> duu;
    private final List<BoundFunction> dvv;
    private final List<BoundFunction> duv;

    private SurfaceFrame(Interval u, Interval v, List<String> components, List<String> variables, CompiledObject object) {
        this.u = u;
        this.v = v;
        this.position = new ArrayList<>();
        this.du = new ArrayList<>();
        this.dv = new ArrayList<>();
        this.duu = new ArrayList<>();
        this.dvv = new ArrayList<>();
        this.duv = new ArrayList<>();
        String first = variables.get(0);
        String second = variables.get(1);
        for (String component : components) {
            position.add(object.function(component, variables));
            org.jmouse.formula.Formula alongFirst = object.derivativeExpression(component, first);
            org.jmouse.formula.Formula alongSecond = object.derivativeExpression(component, second);
            du.add(object.function(alongFirst, variables));
            dv.add(object.function(alongSecond, variables));
            duu.add(object.function(object.derivativeExpression(alongFirst, first), variables));
            dvv.add(object.function(object.derivativeExpression(alongSecond, second), variables));
            duv.add(object.function(object.derivativeExpression(alongFirst, second), variables));
        }
    }

    static SurfaceFrame of(CompiledObject object) {
        return switch (object.definition()) {
            case ExplicitSurface surface -> new SurfaceFrame(surface.x(), surface.y(), List.of(surface.variables().get(0),
                    surface.variables().get(1), surface.expression()), surface.variables(), object);
            case ParametricSurface surface when surface.components().size() == 3 ->
                    new SurfaceFrame(surface.u(), surface.v(), surface.components(), surface.variables(), object);
            default -> throw new IllegalArgumentException("A surface frame needs a surface in R³.");
        };
    }

    Interval u() {
        return u;
    }

    Interval v() {
        return v;
    }

    double[] position(double first, double second) {
        return evaluate(position, first, second);
    }

    double[] unitNormal(double first, double second) {
        double[] normal = cross(evaluate(du, first, second), evaluate(dv, first, second));
        double length = Math.sqrt(AnalysisComputations.dot(normal, normal));
        return length == 0 ? normal : new double[]{normal[0] / length, normal[1] / length, normal[2] / length};
    }

    /** From the first (E, F, G) and second (L, M, N) fundamental forms. */
    double curvature(double first, double second, AnalysisComputations.CurvatureMeasure measure) {
        double[] ru = evaluate(du, first, second);
        double[] rv = evaluate(dv, first, second);
        double[] normal = unitNormal(first, second);
        double e = AnalysisComputations.dot(ru, ru);
        double f = AnalysisComputations.dot(ru, rv);
        double g = AnalysisComputations.dot(rv, rv);
        double l = AnalysisComputations.dot(evaluate(duu, first, second), normal);
        double m = AnalysisComputations.dot(evaluate(duv, first, second), normal);
        double n = AnalysisComputations.dot(evaluate(dvv, first, second), normal);
        double denominator = e * g - f * f;
        double gaussian = (l * n - m * m) / denominator;
        double mean = (e * n - 2 * f * m + g * l) / (2 * denominator);
        double spread = Math.sqrt(Math.max(0, mean * mean - gaussian));
        return switch (measure) {
            case GAUSSIAN -> gaussian;
            case MEAN -> mean;
            case PRINCIPAL_MAXIMUM -> mean + spread;
            case PRINCIPAL_MINIMUM -> mean - spread;
        };
    }

    private static double[] evaluate(List<BoundFunction> functions, double first, double second) {
        double[] values = new double[functions.size()];
        for (int index = 0; index < values.length; index++) {
            values[index] = functions.get(index).at(first, second);
        }
        return values;
    }

    private static double[] cross(double[] first, double[] second) {
        return new double[]{first[1] * second[2] - first[2] * second[1], first[2] * second[0] - first[0] * second[2],
                first[0] * second[1] - first[1] * second[0]};
    }
}
