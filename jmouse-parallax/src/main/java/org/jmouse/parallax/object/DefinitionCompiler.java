package org.jmouse.parallax.object;

import org.jmouse.formula.FormulaException;
import org.jmouse.parallax.component.ComponentDocuments;
import org.jmouse.parallax.component.ComponentVocabulary;
import org.jmouse.parallax.object.ObjectDefinition.AffineTransformation;
import org.jmouse.parallax.object.ObjectDefinition.Circuit;
import org.jmouse.parallax.object.ObjectDefinition.Program;
import org.jmouse.parallax.dimension.PolytopeSkeleton;
import org.jmouse.parallax.program.ProgramRun;
import org.jmouse.parallax.region.RegionComputations;
import org.jmouse.parallax.object.ObjectDefinition.ComplexMapping;
import org.jmouse.parallax.object.ObjectDefinition.DifferentialSystem;
import org.jmouse.parallax.object.ObjectDefinition.DiscreteMap;
import org.jmouse.parallax.object.ObjectDefinition.EscapeTimeFractal;
import org.jmouse.parallax.object.ObjectDefinition.ExplicitCurve;
import org.jmouse.parallax.object.ObjectDefinition.ExplicitSurface;
import org.jmouse.parallax.object.ObjectDefinition.GeometricConstruction;
import org.jmouse.parallax.object.ObjectDefinition.ImplicitCurve;
import org.jmouse.parallax.object.ObjectDefinition.ImplicitHypersurface;
import org.jmouse.parallax.object.ObjectDefinition.ImplicitSurface;
import org.jmouse.parallax.object.ObjectDefinition.IteratedFunctionSystem;
import org.jmouse.parallax.object.ObjectDefinition.LindenmayerSystem;
import org.jmouse.parallax.object.ObjectDefinition.MatrixObject;
import org.jmouse.parallax.object.ObjectDefinition.NewtonFractal;
import org.jmouse.parallax.object.ObjectDefinition.NonlinearTransformation;
import org.jmouse.parallax.object.ObjectDefinition.ParametricCurve;
import org.jmouse.parallax.object.ObjectDefinition.ParametricSurface;
import org.jmouse.parallax.object.ObjectDefinition.PointSet;
import org.jmouse.parallax.object.ObjectDefinition.Polytope;
import org.jmouse.parallax.object.ObjectDefinition.ProbabilityDistribution;
import org.jmouse.parallax.object.ObjectDefinition.Sequence;
import org.jmouse.parallax.object.ObjectDefinition.Region;
import org.jmouse.parallax.object.ObjectDefinition.VectorField;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Checks a definition against its parameters and compiles it. Every expression of the definition is bound to the
 * variables its kind declares, so an unknown name, a typo or a function without a meaning here is refused when
 * the object is saved — not when somebody first opens it.
 */
public final class DefinitionCompiler {

    public static final List<String> PLANE = List.of("x", "y");
    public static final List<String> SPACE = List.of("x", "y", "z");
    public static final List<String> SPACE_TIME = List.of("x", "y", "z", "w");

    private DefinitionCompiler() {
    }

    public static CompiledObject compile(ObjectDefinition definition, List<ParameterDefinition> parameters,
                                         Map<String, Double> values) {
        return compile(definition, parameters, values, ComponentLibrary.empty());
    }

    /** Compiles with a library of component models a circuit may reference, besides the built-in ones. */
    public static CompiledObject compile(ObjectDefinition definition, List<ParameterDefinition> parameters,
                                         Map<String, Double> values, ComponentLibrary library) {
        requireDistinctNames(definition, parameters);
        ParameterDefinition.requireKnown(parameters, values);
        CompiledObject compiled = new CompiledObject(definition, parameters, ParameterDefinition.resolve(parameters, values),
                library);
        checkExpressions(compiled);
        return compiled;
    }

    /**
     * The variables a definition's expressions are written in, as its author named them; parameters may not take these
     * names.
     */
    public static List<String> variablesOf(ObjectDefinition definition) {
        return switch (definition) {
            case ExplicitCurve curve -> curve.variables();
            case ExplicitSurface surface -> surface.variables();
            case ImplicitCurve curve -> curve.variables();
            case ImplicitSurface surface -> surface.variables();
            case ImplicitHypersurface hypersurface -> SPACE_TIME;
            case ParametricCurve curve -> curve.variables();
            case ParametricSurface surface -> surface.variables();
            case VectorField field -> field.components().size() == 2 ? PLANE : SPACE;
            case DifferentialSystem system -> withTime(system.variables());
            case DiscreteMap map -> withIndex(map.variables());
            case ComplexMapping mapping -> mapping.variables();
            case EscapeTimeFractal fractal -> fractal.state().equals(fractal.input()) ? List.of(fractal.state())
                    : List.of(fractal.state(), fractal.input());
            case NewtonFractal fractal -> List.of(fractal.state());
            case NonlinearTransformation transformation -> transformation.components().size() == 2 ? PLANE : SPACE;
            case ProbabilityDistribution distribution -> distribution.variables();
            case Sequence sequence -> sequence.variables();
            case Region region -> region.variables();
            case ObjectDefinition.Value value -> List.of();
            case IteratedFunctionSystem system -> List.of();
            case LindenmayerSystem system -> List.of();
            case MatrixObject matrix -> List.of();
            case AffineTransformation transformation -> List.of();
            case PointSet points -> List.of();
            case GeometricConstruction construction -> List.of();
            case Polytope polytope -> List.of();
            case Circuit circuit -> List.of();
            case Program program -> List.of();
            case ObjectDefinition.Component component -> List.of();
            case ObjectDefinition.Symbol symbol -> List.of();
            case ObjectDefinition.Graph graph -> List.of();
            case ObjectDefinition.Family family -> variablesOf(family.member());
        };
    }

    private static List<String> withTime(List<String> variables) {
        List<String> all = new ArrayList<>(variables);
        all.add("t");
        return all;
    }

    private static List<String> withIndex(List<String> variables) {
        List<String> all = new ArrayList<>(variables);
        all.add("n");
        return all;
    }

    private static void requireDistinctNames(ObjectDefinition definition, List<ParameterDefinition> parameters) {
        Set<String> variables = new HashSet<>(variablesOf(definition));
        Set<String> seen = new HashSet<>();
        for (ParameterDefinition parameter : parameters) {
            if (variables.contains(parameter.name())) {
                throw new FormulaException("'" + parameter.name() + "' is a variable of this kind and cannot also be a parameter.",
                        null, -1);
            }
            if (!seen.add(parameter.name())) {
                throw new FormulaException("The parameter '" + parameter.name() + "' is declared twice.", null, -1);
            }
        }
        List<String> state = switch (definition) {
            case DifferentialSystem system -> system.variables();
            case DiscreteMap map -> map.variables();
            default -> List.of();
        };
        if (new HashSet<>(state).size() != state.size()) {
            throw new FormulaException("A state variable is named twice.", null, -1);
        }
    }

    private static void checkExpressions(CompiledObject compiled) {
        ObjectDefinition definition = compiled.definition();
        List<String> variables = variablesOf(definition);
        switch (definition) {
            case ExplicitCurve curve -> compiled.function(curve.expression(), variables);
            case ExplicitSurface surface -> compiled.function(surface.expression(), variables);
            case ImplicitCurve curve -> compiled.function(curve.expression(), variables);
            case ImplicitSurface surface -> compiled.function(surface.expression(), variables);
            case ImplicitHypersurface hypersurface -> compiled.function(hypersurface.expression(), variables);
            case ParametricCurve curve -> compiled.functions(curve.components(), variables);
            case ParametricSurface surface -> compiled.functions(surface.components(), variables);
            case VectorField field -> compiled.functions(field.components(), variables);
            case DifferentialSystem system -> {
                system.equations().forEach(equation -> compiled.stochasticFunction(equation, variables, true));
                system.starts().forEach(compiled::constant);
            }
            case DiscreteMap map -> checkMap(compiled, map, variables);
            case ComplexMapping mapping -> compiled.complexFunction(mapping.expression(), variables);
            case EscapeTimeFractal fractal -> {
                compiled.complexFunction(fractal.iteration(), variables);
                compiled.complexFunction(fractal.start(), List.of(fractal.input()));
            }
            case NewtonFractal fractal -> compiled.complexFunction(fractal.iteration(), variables);
            case NonlinearTransformation transformation -> compiled.functions(transformation.components(), variables);
            case ProbabilityDistribution distribution -> compiled.function(distribution.density(), variables);
            case Region region -> RegionComputations.check(compiled);
            case Sequence sequence -> compiled.function(sequence.expression(), variables);
            case ObjectDefinition.Value value -> compiled.constant(value.expression());
            case MatrixObject matrix -> matrix.entries().forEach(row -> row.forEach(compiled::constant));
            case AffineTransformation transformation -> {
                transformation.matrix().forEach(row -> row.forEach(compiled::constant));
                transformation.translation().forEach(compiled::constant);
            }
            case GeometricConstruction construction -> checkConstruction(compiled, construction);
            case LindenmayerSystem system -> checkLindenmayer(system);
            case IteratedFunctionSystem system -> {
            }
            case PointSet points -> {
            }
            case Polytope polytope -> PolytopeSkeleton.of(polytope);
            case Circuit circuit -> checkCircuit(compiled, circuit);
            case Program program -> ProgramRun.check(compiled, program);
            case ObjectDefinition.Component component -> {
            }
            case ObjectDefinition.Family family -> {
                // each member is compiled, and so checked, where the family is computed
            }
            case ObjectDefinition.Graph graph -> {
                // each line is read where the Graph classifies it, not here
            }
            case ObjectDefinition.Symbol symbol -> {
            }
        }
    }

    private static void checkCircuit(CompiledObject compiled, Circuit circuit) {
        for (ObjectDefinition.CircuitElement element : circuit.elements()) {
            if (element.type() == ObjectDefinition.CircuitElementType.MODEL) {
                element.overrides().values().forEach(compiled::constant);
                compiled.componentSource(element.component());
                continue;
            }
            compiled.constant(element.value());
            if (!element.overrides().isEmpty()) {
                checkPrimitiveOverrides(compiled, element);
            }
        }
    }

    /**
     * A primitive may set any parameter its component declares, and the bench's own figures — temperature, realistic,
     * the tolerance corner — which every part reads and a bench other than nominal gives each of them.
     */
    private static void checkPrimitiveOverrides(CompiledObject compiled, ObjectDefinition.CircuitElement element) {
        String reference = element.type().primitiveComponent();
        Set<String> declared = new HashSet<>();
        ComponentDocuments.read(compiled.componentSource(reference), reference, compiled.library())
                .parameters().forEach(parameter -> declared.add(parameter.name()));
        Set<String> bench = ComponentVocabulary.nominalBench().keySet();
        element.overrides().forEach((name, expression) -> {
            if (!declared.contains(name) && !bench.contains(name)) {
                throw new IllegalArgumentException("Part '" + element.id() + "': '" + reference + "' has no parameter '" + name
                        + "'; it has " + String.join(", ", new TreeSet<>(declared)) + ".");
            }
            compiled.constant(expression);
        });
    }

    private static void checkConstruction(CompiledObject compiled, GeometricConstruction construction) {
        Set<String> defined = new HashSet<>();
        for (ObjectDefinition.GeometricElement element : construction.elements()) {
            for (String reference : element.references()) {
                if (!defined.contains(reference)) {
                    throw new FormulaException("Element '" + element.id() + "' refers to '" + reference
                            + "', which is not defined before it.", null, -1);
                }
            }
            if (element.type() == ObjectDefinition.GeometricElementType.POINT
                    && element.coordinates().size() != construction.dimension()) {
                throw new FormulaException("Point '" + element.id() + "' needs " + construction.dimension()
                        + " coordinates.", null, -1);
            }
            element.coordinates().forEach(compiled::constant);
            if (!defined.add(element.id())) {
                throw new FormulaException("Element '" + element.id() + "' is defined twice.", null, -1);
            }
        }
    }

    private static void checkLindenmayer(LindenmayerSystem system) {
        for (Map.Entry<String, String> rule : system.rules().entrySet()) {
            if (rule.getKey().length() != 1) {
                throw new FormulaException("An L-system rule rewrites one symbol, not '" + rule.getKey() + "'.", null, -1);
            }
        }
    }

    /**
     * Each step value over the state, {@code n} and the values before it; each update over all of them — random
     * functions allowed, so a random walk compiles here as a logistic map does.
     */
    private static void checkMap(CompiledObject compiled, DiscreteMap map, List<String> variables) {
        List<String> names = new ArrayList<>(variables);
        for (ObjectDefinition.StepValue value : map.stepValues()) {
            compiled.stochasticFunction(value.expression(), List.copyOf(names), false);
            names.add(value.name());
        }
        map.updates().forEach(update -> compiled.stochasticFunction(update, names, false));
        map.starts().forEach(compiled::constant);
    }
}
