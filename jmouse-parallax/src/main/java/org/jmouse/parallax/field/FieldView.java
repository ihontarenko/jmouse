package org.jmouse.parallax.field;

import org.jmouse.formula.FormulaPrinter;
import org.jmouse.parallax.object.BoundFunction;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.Interval;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.DifferentialSystem;
import org.jmouse.parallax.object.ObjectDefinition.VectorField;

import java.util.ArrayList;
import java.util.List;

/**
 * A vector field over a box, whichever object it comes from: a vector field itself, or the direction field of a
 * two- or three-dimensional system (read at t = t₀, so an autonomous system is exactly its field).
 */
public final class FieldView {

    private final int dimension;
    private final List<Interval> bounds;
    private final List<BoundFunction> components;
    private final List<String> sources;
    private final List<String> variables;
    private final boolean withTime;
    private final double time;
    private final CompiledObject object;

    private FieldView(CompiledObject object, List<String> sources, List<String> variables, List<Interval> bounds,
                      boolean withTime, double time) {
        this.object = object;
        this.dimension = sources.size();
        this.sources = sources;
        this.variables = variables;
        this.bounds = bounds;
        this.withTime = withTime;
        this.time = time;
        this.components = new ArrayList<>();
        for (String source : sources) {
            components.add(object.function(source, variables));
        }
    }

    public static boolean admits(ObjectDefinition definition) {
        return definition instanceof VectorField
                || (definition instanceof DifferentialSystem system && system.variables().size() >= 2
                && system.variables().size() <= 3 && !system.bounds().isEmpty());
    }

    public static FieldView of(CompiledObject object) {
        return switch (object.definition()) {
            case VectorField field -> new FieldView(object, field.components(),
                    field.components().size() == 2 ? List.of("x", "y") : List.of("x", "y", "z"), field.bounds(), false, 0);
            case DifferentialSystem system -> {
                List<String> variables = new ArrayList<>(system.variables());
                variables.add("t");
                yield new FieldView(object, system.equations(), variables, system.bounds(), true, system.time().minimum());
            }
            default -> throw new IllegalArgumentException("This object is not a field.");
        };
    }

    public int dimension() {
        return dimension;
    }

    public List<Interval> bounds() {
        return bounds;
    }

    public void evaluate(double[] point, double[] target) {
        for (int axis = 0; axis < dimension; axis++) {
            BoundFunction component = components.get(axis);
            target[axis] = switch (dimension) {
                case 2 -> withTime ? component.at(point[0], point[1], time) : component.at(point[0], point[1]);
                default -> withTime ? component.at(point[0], point[1], point[2], time) : component.at(point[0], point[1], point[2]);
            };
        }
    }

    /** ∂Fᵢ/∂xⱼ, exact. */
    public BoundFunction partial(int component, int axis) {
        return object.function(object.derivativeExpression(sources.get(component), variables.get(axis)), variables);
    }

    public double partialAt(BoundFunction partial, double[] point) {
        return switch (dimension) {
            case 2 -> withTime ? partial.at(point[0], point[1], time) : partial.at(point[0], point[1]);
            default -> withTime ? partial.at(point[0], point[1], point[2], time) : partial.at(point[0], point[1], point[2]);
        };
    }

    public String componentText(int component) {
        return FormulaPrinter.text(components.get(component).expression());
    }

    public boolean inside(double[] point) {
        for (int axis = 0; axis < dimension; axis++) {
            if (!bounds.get(axis).contains(point[axis])) {
                return false;
            }
        }
        return true;
    }
}
