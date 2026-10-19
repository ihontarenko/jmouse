package org.jmouse.parallax.component;

import org.jmouse.el.type.ValueType;
import org.jmouse.formula.Formula;
import org.jmouse.formula.MathematicalFunction;
import org.jmouse.parallax.script.Dimension;
import org.jmouse.parallax.script.QuantityType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Whether a component's expressions agree with what they claim to be: a current equation's right side a current, a
 * voltage equation's a voltage, a typed {@code let}'s what its type says, a display or a meter what its unit says, and
 * no sum adds a voltage to a current.
 *
 * <p>⚠️ <strong>Conservative by construction.</strong> A figure is refused only where both sides are KNOWN and
 * disagree. A pin or a node is a voltage, {@code pin.current} a current, {@code d(q)/dt} what {@code q} is over a
 * second, {@code time} a time, {@code temperature} a temperature, a parameter what its written unit says; a parameter
 * written without a unit and a call into a formula library are UNKNOWN, and anything they touch is too. A bare number
 * is FREE: a pure number in a product, and whatever the other side is in a sum — {@code 1 + tc * (T - T_nominal)}
 * reads as written. So a model whose parameters carry no units is never refused; one that writes its units is held
 * to them.</p>
 *
 * <p>Nothing here reaches the solver: it reads the expressions once, when the model is read, and is gone.</p>
 */
final class DimensionCheck {

    /** What an expression is known to be. */
    private sealed interface Measure {
        /** Measured in this dimension. */
        record Known(Dimension dimension) implements Measure {
        }

        /** A bare number: a pure number in a product, anything in a sum. */
        record Free() implements Measure {
        }

        /** Nothing can be said: a parameter without a unit, a library function's result. */
        record Unknown() implements Measure {
        }
    }

    private static final Measure FREE = new Measure.Free();
    private static final Measure UNKNOWN = new Measure.Unknown();

    /** Functions of a pure number that answer a pure number: an exponential of a voltage is a mistake. */
    private static final Set<MathematicalFunction> OF_PURE_NUMBERS = Set.of(
            MathematicalFunction.SIN, MathematicalFunction.COS, MathematicalFunction.TAN, MathematicalFunction.COT,
            MathematicalFunction.SEC, MathematicalFunction.CSC, MathematicalFunction.ASIN, MathematicalFunction.ACOS,
            MathematicalFunction.ATAN, MathematicalFunction.SINH, MathematicalFunction.COSH, MathematicalFunction.TANH,
            MathematicalFunction.ASINH, MathematicalFunction.ACOSH, MathematicalFunction.ATANH, MathematicalFunction.EXP,
            MathematicalFunction.LN, MathematicalFunction.GAMMA, MathematicalFunction.FACTORIAL,
            MathematicalFunction.BINOMIAL, MathematicalFunction.ERF);

    /** Functions whose answer is what their arguments are, which must agree among themselves. */
    private static final Set<MathematicalFunction> PRESERVING = Set.of(
            MathematicalFunction.ABS, MathematicalFunction.MIN, MathematicalFunction.MAX, MathematicalFunction.FLOOR,
            MathematicalFunction.CEIL, MathematicalFunction.ROUND, MathematicalFunction.HYPOT, MathematicalFunction.MOD);

    private final ComponentModel model;
    private final List<Formula.Definition> library;
    private final Map<String, Measure> names = new HashMap<>();
    private int line;

    private DimensionCheck(ComponentModel model, List<Formula.Definition> library) {
        this.model = model;
        this.library = library;
    }

    /**
     * Refuses a component one of whose expressions is not what it claims to be, naming the line and both sides.
     *
     * @throws IllegalArgumentException at the first expression that disagrees
     */
    static void require(ComponentModel model) {
        if (model.family() || model.equations().isEmpty() && model.displays().isEmpty() && model.measures().isEmpty()) {
            return;
        }
        List<Formula.Definition> library = ModelLanguage.LANGUAGE.definitions(ModelLanguage.libraryText(model));
        new DimensionCheck(model, library).check();
    }

    private void check() {
        model.pins().forEach(pin -> names.put(pin.name(), known(Dimension.VOLTAGE)));
        model.nodes().forEach(node -> names.put(node, known(Dimension.VOLTAGE)));
        names.put(ComponentVocabulary.TIME, known(Dimension.TIME));
        names.put(ComponentVocabulary.TEMPERATURE, known(Dimension.TEMPERATURE));
        names.put(ComponentVocabulary.REALISTIC, known(Dimension.NONE));
        model.parameters().forEach(parameter -> names.put(parameter.name(),
                Dimension.ofUnit(parameter.unit()).<Measure>map(DimensionCheck::known).orElse(UNKNOWN)));
        model.states().forEach(state -> names.put(state.name(), UNKNOWN));
        model.locals().forEach((name, text) -> {
            Measure measured = measure(read(text, 0));
            ValueType declared = model.localTypes().get(name);
            if (declared instanceof QuantityType quantity) {
                require(measured, quantity.dimension(), "'let " + name + " : " + quantity.name() + "'");
                measured = known(quantity.dimension());
            }
            names.put(name, measured);
        });
        for (ComponentModel.Equation equation : model.equations()) {
            line = equation.line();
            Measure right = measure(read(equation.formula(), equation.line()));
            switch (equation.kind()) {
                case CURRENT -> require(right, Dimension.CURRENT, "'current " + equation.positive() + " -> "
                        + equation.negative() + "'");
                case VOLTAGE -> require(right, Dimension.VOLTAGE, "'voltage " + equation.positive() + " -> "
                        + equation.negative() + "'");
                case STATE -> {
                    if (names.get(equation.state()) instanceof Measure.Known state) {
                        require(right, state.dimension().over(Dimension.TIME), "'d(" + equation.state() + ")/dt'");
                    }
                }
            }
        }
        line = 0;
        model.displays().forEach((name, text) -> unitOf(model.displayUnits().get(name)).ifPresent(dimension ->
                require(measure(read(text, 0)), dimension, "the display '" + name + "', in "
                        + model.displayUnits().get(name) + ",")));
        model.measures().forEach((name, figure) -> unitOf(figure.unit()).ifPresent(dimension ->
                require(measure(read(figure.expression(), 0)), dimension, "the reading '" + name + "', in "
                        + figure.unit() + ",")));
    }

    private Formula read(String text, int at) {
        return ModelLanguage.read(model.name(), text, at, library);
    }

    private static Optional<Dimension> unitOf(String unit) {
        return unit == null || unit.isBlank() ? Optional.empty() : Dimension.ofUnit(unit);
    }

    private void require(Measure measured, Dimension expected, String what) {
        if (measured instanceof Measure.Known known && !known.dimension().equals(expected)) {
            throw refusal(what + " is " + expected.describe() + ", but its expression is " + known.dimension().describe());
        }
    }

    private Measure measure(Formula formula) {
        return switch (formula) {
            case Formula.Constant constant -> FREE;
            case Formula.NamedConstant constant -> FREE;
            case Formula.ImaginaryUnit unit -> FREE;
            case Formula.Variable variable -> names.getOrDefault(variable.name(), UNKNOWN);
            case Formula.Negation negation -> measure(negation.operand());
            case Formula.Binary binary -> binary(binary);
            case Formula.Call call -> call(call);
            case Formula.HostCall call -> hostCall(call);
            case Formula.Piecewise piecewise -> {
                Measure result = piecewise.otherwise() == null ? FREE : measure(piecewise.otherwise());
                for (Formula.Piecewise.Branch branch : piecewise.branches()) {
                    result = sum(result, measure(branch.value()), "the branches of a choice");
                }
                yield result;
            }
            case Formula.Comparison comparison -> {
                sum(measure(comparison.left()), measure(comparison.right()), "a comparison");
                yield known(Dimension.NONE);
            }
            default -> UNKNOWN;
        };
    }

    private Measure binary(Formula.Binary binary) {
        Measure left = measure(binary.left());
        Measure right = measure(binary.right());
        return switch (binary.operator()) {
            case ADD, SUBTRACT -> sum(left, right, binary.operator() == Formula.Operator.ADD ? "a sum" : "a difference");
            case MULTIPLY -> product(left, right, false);
            case DIVIDE -> product(left, right, true);
            case POWER -> power(left, binary.right());
        };
    }

    private Measure sum(Measure left, Measure right, String what) {
        if (left instanceof Measure.Known one && right instanceof Measure.Known other
                && !one.dimension().equals(other.dimension())) {
            throw refusal(what + " joins " + one.dimension().describe() + " and " + other.dimension().describe());
        }
        if (left instanceof Measure.Known) {
            return left;
        }
        if (right instanceof Measure.Known) {
            return right;
        }
        return left instanceof Measure.Unknown || right instanceof Measure.Unknown ? UNKNOWN : FREE;
    }

    private static Measure product(Measure left, Measure right, boolean dividing) {
        if (left instanceof Measure.Unknown || right instanceof Measure.Unknown) {
            return UNKNOWN;
        }
        Dimension one = left instanceof Measure.Known known ? known.dimension() : Dimension.NONE;
        Dimension other = right instanceof Measure.Known known ? known.dimension() : Dimension.NONE;
        if (left instanceof Measure.Free && right instanceof Measure.Free) {
            return FREE;
        }
        return known(dividing ? one.over(other) : one.times(other));
    }

    private Measure power(Measure base, Formula exponent) {
        if (base instanceof Measure.Free) {
            return FREE;
        }
        if (!(base instanceof Measure.Known known)) {
            return UNKNOWN;
        }
        if (known.dimension().isNone()) {
            return base;
        }
        if (exponent instanceof Formula.Constant constant && constant.value() == Math.rint(constant.value())) {
            return known(known.dimension().power((int) constant.value()));
        }
        if (exponent instanceof Formula.Constant constant && constant.value() == 0.5) {
            return known.dimension().root().<Measure>map(DimensionCheck::known).orElse(UNKNOWN);
        }
        return UNKNOWN;
    }

    private Measure call(Formula.Call call) {
        List<Measure> arguments = call.arguments().stream().map(this::measure).toList();
        if (OF_PURE_NUMBERS.contains(call.function())) {
            for (Measure argument : arguments) {
                if (argument instanceof Measure.Known known && !known.dimension().isNone()) {
                    throw refusal(call.function().name().toLowerCase() + "(…) takes a pure number, not "
                            + known.dimension().describe());
                }
            }
            return FREE;
        }
        if (PRESERVING.contains(call.function())) {
            Measure result = arguments.isEmpty() ? UNKNOWN : arguments.getFirst();
            for (Measure argument : arguments.subList(Math.min(1, arguments.size()), arguments.size())) {
                result = sum(result, argument, call.function().name().toLowerCase() + "(…)");
            }
            return result;
        }
        if (call.function() == MathematicalFunction.SQRT && arguments.size() == 1) {
            return arguments.getFirst() instanceof Measure.Known known
                    ? known.dimension().root().<Measure>map(DimensionCheck::known).orElse(UNKNOWN)
                    : arguments.getFirst();
        }
        if (call.function() == MathematicalFunction.SIGN || call.function() == MathematicalFunction.STEP) {
            return FREE;
        }
        return UNKNOWN;
    }

    private Measure hostCall(Formula.HostCall call) {
        return switch (call.name()) {
            case PinQuantities.CURRENT -> known(Dimension.CURRENT);
            case PinQuantities.VOLTAGE -> known(Dimension.VOLTAGE);
            case ModelLanguage.RATE -> {
                Measure changed = call.arguments().isEmpty() ? UNKNOWN : measure(call.arguments().getFirst());
                yield changed instanceof Measure.Known known ? known(known.dimension().over(Dimension.TIME)) : changed;
            }
            default -> UNKNOWN;
        };
    }

    private static Measure known(Dimension dimension) {
        return new Measure.Known(dimension);
    }

    private IllegalArgumentException refusal(String detail) {
        return new IllegalArgumentException(model.name() + (line > 0 ? ", line " + line : "") + ": " + detail + ".");
    }
}
