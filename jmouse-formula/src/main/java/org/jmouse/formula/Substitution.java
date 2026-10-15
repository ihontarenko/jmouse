package org.jmouse.formula;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Replaces free names by expressions without capturing anything: where a replacement mentions a name that a
 * sum, a product or an integral binds, that binder is renamed first. {@code sum(k, 1, 3, x k)} with x := k
 * becomes {@code sum(k', 1, 3, k k')}, not {@code sum(k, 1, 3, k k)}.
 *
 * <p>A {@link Formula.Definition}'s body is never rewritten: it is lexically scoped, so what it leaves free
 * belongs to the top level, not to whatever is substituted at a call site.
 */
public final class Substitution {

    private Substitution() {
    }

    public static Formula substitute(Formula expression, String name, Formula replacement) {
        return substitute(expression, Map.of(name, replacement));
    }

    public static Formula substitute(Formula expression, Map<String, Formula> replacements) {
        if (replacements.isEmpty()) {
            return expression;
        }
        return switch (expression) {
            case Formula.Variable variable -> replacements.getOrDefault(variable.name(), variable);
            case Formula.Slot slot -> slot;
            case Formula.HostCall hostCall -> new Formula.HostCall(hostCall.name(),
                    hostCall.arguments().stream().map(argument -> substitute(argument, replacements)).toList());
            case Formula.Constant constant -> constant;
            case Formula.NamedConstant named -> named;
            case Formula.ImaginaryUnit unit -> unit;
            case Formula.Negation negation -> new Formula.Negation(substitute(negation.operand(), replacements));
            case Formula.Not not -> new Formula.Not(substitute(not.operand(), replacements));
            case Formula.Binary binary -> new Formula.Binary(binary.operator(), substitute(binary.left(), replacements),
                    substitute(binary.right(), replacements));
            case Formula.Comparison comparison -> new Formula.Comparison(comparison.relation(),
                    substitute(comparison.left(), replacements), substitute(comparison.right(), replacements));
            case Formula.Logical logical -> new Formula.Logical(logical.connective(),
                    substitute(logical.left(), replacements), substitute(logical.right(), replacements));
            case Formula.Equation equation -> new Formula.Equation(substitute(equation.left(), replacements),
                    substitute(equation.right(), replacements));
            case Formula.Call call -> new Formula.Call(call.function(),
                    call.arguments().stream().map(argument -> substitute(argument, replacements)).toList());
            case Formula.Application application -> new Formula.Application(application.definition(),
                    application.arguments().stream().map(argument -> substitute(argument, replacements)).toList());
            case Formula.Piecewise piecewise -> new Formula.Piecewise(piecewise.branches().stream()
                    .map(branch -> new Formula.Piecewise.Branch(substitute(branch.condition(), replacements),
                            substitute(branch.value(), replacements))).toList(),
                    piecewise.otherwise() == null ? null : substitute(piecewise.otherwise(), replacements));
            case Formula.Accumulation accumulation -> {
                Bound bound = underBinder(accumulation.index(), accumulation.body(), replacements);
                yield new Formula.Accumulation(accumulation.accumulator(), bound.name(),
                        substitute(accumulation.from(), replacements), substitute(accumulation.to(), replacements),
                        bound.body());
            }
            case Formula.Integral integral -> {
                Bound bound = underBinder(integral.variable(), integral.body(), replacements);
                yield new Formula.Integral(bound.name(), substitute(integral.lower(), replacements),
                        substitute(integral.upper(), replacements), bound.body());
            }
        };
    }

    /**
     * Every {@link Formula.HostCall} replaced by what its host makes of it, inside the bodies of the definitions the
     * expression calls as well — {@code V_diode = V(j, cathode)} is a definition, and its voltage is the host's too.
     * Calls are replaced from the inside out, so {@code binding} sees a call whose arguments are already bound; each
     * definition is rewritten once, however many times it is called.
     */
    public static Formula replaceHostCalls(Formula expression, Function<Formula.HostCall, Formula> binding) {
        return new HostBinding(binding).rewrite(expression);
    }

    /**
     * Every free name the host supplies replaced by what the host makes of it — a circuit model's pins, whose names
     * read their voltages —, inside the bodies of the definitions the expression calls as well, as
     * {@link #replaceHostCalls} does: {@code local drive = (target - output) / R_output} is a definition, and its
     * {@code output} is the pin's. A name a definition's parameter, a sum's index or an integral's variable binds is that
     * binder's, never the host's. {@code binding} answers null for a name it does not supply.
     */
    public static Formula bindHostNames(Formula expression, Function<String, Formula> binding) {
        return new NameBinding(binding).rewrite(expression, Set.of());
    }

    private static final class NameBinding {

        private final Function<String, Formula>                      binding;
        private final Map<Formula.Definition, Formula.Definition>  rewritten = new IdentityHashMap<>();

        NameBinding(Function<String, Formula> binding) {
            this.binding = binding;
        }

        Formula rewrite(Formula expression, Set<String> bound) {
            return switch (expression) {
                case Formula.Variable variable -> {
                    Formula supplied = bound.contains(variable.name()) ? null : binding.apply(variable.name());
                    yield supplied == null ? variable : supplied;
                }
                case Formula.HostCall hostCall -> new Formula.HostCall(hostCall.name(),
                        hostCall.arguments().stream().map(argument -> rewrite(argument, bound)).toList());
                case Formula.Slot slot -> slot;
                case Formula.Constant constant -> constant;
                case Formula.NamedConstant named -> named;
                case Formula.ImaginaryUnit unit -> unit;
                case Formula.Negation negation -> new Formula.Negation(rewrite(negation.operand(), bound));
                case Formula.Not not -> new Formula.Not(rewrite(not.operand(), bound));
                case Formula.Binary binary -> new Formula.Binary(binary.operator(), rewrite(binary.left(), bound),
                        rewrite(binary.right(), bound));
                case Formula.Comparison comparison -> new Formula.Comparison(comparison.relation(),
                        rewrite(comparison.left(), bound), rewrite(comparison.right(), bound));
                case Formula.Logical logical -> new Formula.Logical(logical.connective(), rewrite(logical.left(), bound),
                        rewrite(logical.right(), bound));
                case Formula.Equation equation -> new Formula.Equation(rewrite(equation.left(), bound),
                        rewrite(equation.right(), bound));
                case Formula.Call call -> new Formula.Call(call.function(),
                        call.arguments().stream().map(argument -> rewrite(argument, bound)).toList());
                case Formula.Application application -> new Formula.Application(definition(application.definition()),
                        application.arguments().stream().map(argument -> rewrite(argument, bound)).toList());
                case Formula.Piecewise piecewise -> new Formula.Piecewise(piecewise.branches().stream()
                        .map(branch -> new Formula.Piecewise.Branch(rewrite(branch.condition(), bound),
                                rewrite(branch.value(), bound))).toList(),
                        piecewise.otherwise() == null ? null : rewrite(piecewise.otherwise(), bound));
                case Formula.Accumulation accumulation -> new Formula.Accumulation(accumulation.accumulator(),
                        accumulation.index(), rewrite(accumulation.from(), bound), rewrite(accumulation.to(), bound),
                        rewrite(accumulation.body(), with(bound, accumulation.index())));
                case Formula.Integral integral -> new Formula.Integral(integral.variable(), rewrite(integral.lower(), bound),
                        rewrite(integral.upper(), bound), rewrite(integral.body(), with(bound, integral.variable())));
            };
        }

        /** A definition's body sees its own parameters, and the top level's names only through them. */
        private Formula.Definition definition(Formula.Definition definition) {
            Formula.Definition done = rewritten.get(definition);
            if (done == null) {
                done = new Formula.Definition(definition.name(), definition.parameters(),
                        rewrite(definition.body(), Set.copyOf(definition.parameters())));
                rewritten.put(definition, done);
            }
            return done;
        }

        private static Set<String> with(Set<String> bound, String name) {
            Set<String> inner = new HashSet<>(bound);
            inner.add(name);
            return inner;
        }
    }

    /** Whether an unbound host call is left anywhere in the expression or the bodies it calls. */
    static boolean findsHostCall(Formula expression) {
        boolean[] found = {false};
        replaceHostCalls(expression, hostCall -> {
            found[0] = true;
            return hostCall;
        });
        return found[0];
    }

    private static final class HostBinding {

        private final Function<Formula.HostCall, Formula>            binding;
        private final Map<Formula.Definition, Formula.Definition>  rewritten = new IdentityHashMap<>();

        HostBinding(Function<Formula.HostCall, Formula> binding) {
            this.binding = binding;
        }

        Formula rewrite(Formula expression) {
            return switch (expression) {
                case Formula.HostCall hostCall -> binding.apply(new Formula.HostCall(hostCall.name(),
                        hostCall.arguments().stream().map(this::rewrite).toList()));
                case Formula.Variable variable -> variable;
                case Formula.Slot slot -> slot;
                case Formula.Constant constant -> constant;
                case Formula.NamedConstant named -> named;
                case Formula.ImaginaryUnit unit -> unit;
                case Formula.Negation negation -> new Formula.Negation(rewrite(negation.operand()));
                case Formula.Not not -> new Formula.Not(rewrite(not.operand()));
                case Formula.Binary binary -> new Formula.Binary(binary.operator(), rewrite(binary.left()), rewrite(binary.right()));
                case Formula.Comparison comparison -> new Formula.Comparison(comparison.relation(),
                        rewrite(comparison.left()), rewrite(comparison.right()));
                case Formula.Logical logical -> new Formula.Logical(logical.connective(), rewrite(logical.left()),
                        rewrite(logical.right()));
                case Formula.Equation equation -> new Formula.Equation(rewrite(equation.left()), rewrite(equation.right()));
                case Formula.Call call -> new Formula.Call(call.function(), call.arguments().stream().map(this::rewrite).toList());
                case Formula.Application application -> new Formula.Application(definition(application.definition()),
                        application.arguments().stream().map(this::rewrite).toList());
                case Formula.Piecewise piecewise -> new Formula.Piecewise(piecewise.branches().stream()
                        .map(branch -> new Formula.Piecewise.Branch(rewrite(branch.condition()), rewrite(branch.value())))
                        .toList(), piecewise.otherwise() == null ? null : rewrite(piecewise.otherwise()));
                case Formula.Accumulation accumulation -> new Formula.Accumulation(accumulation.accumulator(),
                        accumulation.index(), rewrite(accumulation.from()), rewrite(accumulation.to()),
                        rewrite(accumulation.body()));
                case Formula.Integral integral -> new Formula.Integral(integral.variable(), rewrite(integral.lower()),
                        rewrite(integral.upper()), rewrite(integral.body()));
            };
        }

        private Formula.Definition definition(Formula.Definition definition) {
            Formula.Definition done = rewritten.get(definition);
            if (done == null) {
                done = new Formula.Definition(definition.name(), definition.parameters(), rewrite(definition.body()));
                rewritten.put(definition, done);
            }
            return done;
        }
    }

    private record Bound(String name, Formula body) {
    }

    /** The body of a binder after substitution, with the binder renamed when a replacement would be captured. */
    private static Bound underBinder(String name, Formula body, Map<String, Formula> replacements) {
        Map<String, Formula> inner = new HashMap<>(replacements);
        inner.remove(name);
        if (inner.isEmpty()) {
            return new Bound(name, body);
        }
        Set<String> bodyFree = body.variables();
        boolean captured = inner.entrySet().stream().anyMatch(entry -> bodyFree.contains(entry.getKey())
                && entry.getValue().dependsOn(name));
        if (!captured) {
            return new Bound(name, substitute(body, inner));
        }
        Set<String> taken = new LinkedHashSet<>(bodyFree);
        inner.values().forEach(replacement -> taken.addAll(replacement.variables()));
        String fresh = freshName(name, taken);
        Formula renamed = substitute(body, name, new Formula.Variable(fresh));
        return new Bound(fresh, substitute(renamed, inner));
    }

    static String freshName(String name, Set<String> taken) {
        String candidate = name + "'";
        while (taken.contains(candidate)) {
            candidate = candidate + "'";
        }
        return candidate;
    }
}
