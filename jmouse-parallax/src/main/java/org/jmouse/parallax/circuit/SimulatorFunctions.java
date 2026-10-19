package org.jmouse.parallax.circuit;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Extension;
import org.jmouse.el.extension.Function;

import java.util.List;

/**
 * What a behaviour may call besides jMS's own vocabulary, and what those calls reach: the unit whose handler is running,
 * and the state the handler reads. Nothing reaches outside the unit.
 */
final class SimulatorFunctions {

    private SimulatorFunctions() {
    }

    /** What the simulator's functions reach while a handler runs. */
    record Scope(DigitalUnit unit, double[] state) {
    }

    /**
     * What a behaviour may call besides jMS's own vocabulary: {@code drive}, {@code schedule}, {@code since},
     * {@code V}, {@code item}, {@code with} and the family's {@code delay_scale}. Nothing reaches outside the unit.
     */
    static final class Functions implements Extension {
        @Override
        public List<Function> getFunctions() {
            return List.of(
                    function("drive", (arguments, scope) -> {
                        String pin = String.valueOf(arguments.get(0));
                        DigitalDriver driver = scope.unit().drivers.get(pin);
                        if (driver == null) {
                            throw new IllegalArgumentException(scope.unit().model.model().name() + ": drive('" + pin + "') names no digital output or internal node.");
                        }
                        double delay = arguments.size() > 2 ? number(arguments.get(2)) : 0;
                        driver.schedule(scope.unit().simulation.now + Math.max(delay, 0), LogicLevel.of(arguments.get(1)));
                        return null;
                    }),
                    function("schedule", (arguments, scope) -> {
                        scope.unit().timers.put(String.valueOf(arguments.get(0)), scope.unit().simulation.now + Math.max(number(arguments.get(1)), 0));
                        return null;
                    }),
                    function("since", (arguments, scope) ->
                            scope.unit().simulation.now - scope.unit().lastChange.getOrDefault(String.valueOf(arguments.get(0)), Double.NEGATIVE_INFINITY)),
                    function("item", (arguments, scope) -> DigitalUnit.arrayOf(arguments.get(0))[(int) number(arguments.get(1))]),
                    function("with", (arguments, scope) -> {
                        Object[] copy = DigitalUnit.arrayOf(arguments.get(0)).clone();
                        copy[(int) number(arguments.get(1))] = arguments.get(2);
                        return copy;
                    }),
                    function("delay_scale", (arguments, scope) -> scope.unit().delayScale(number(arguments.get(0)))));
        }

        private interface Body {
            Object apply(Arguments arguments, Scope scope);
        }

        private static Function function(String name, Body body) {
            return new Function() {
                @Override
                public Object execute(Arguments arguments, EvaluationContext context) {
                    return body.apply(arguments, (Scope) context.getValue(EventSimulation.SCOPE));
                }

                @Override
                public String getName() {
                    return name;
                }
            };
        }

        private static double number(Object value) {
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            throw new IllegalArgumentException("'" + value + "' is not a number.");
        }
    }
}
