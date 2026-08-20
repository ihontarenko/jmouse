package org.jmouse.access.el.condition;

import org.jmouse.access.policy.PolicyException;
import org.jmouse.access.spi.ConditionFunctionFailure;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Function;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Every function a condition may call, and the one place all three readers of that list agree.
 *
 * <h2>Why a catalogue rather than a list</h2>
 *
 * <p>The set of callable names is needed three times, and the three must not be able to differ:
 *
 * <ul>
 *   <li>the <strong>compiler</strong> builds its language from it, so a call has something to resolve;
 *   <li>the <strong>load-time check</strong> reads it to refuse a name nobody registered — a validator
 *       working from its own list drifts one commit behind, starts refusing rules that work, and is
 *       then switched off by somebody who has had enough;
 *   <li>a <strong>policy editor</strong> offers from it, so what can be typed and what can be run are
 *       the same set.
 * </ul>
 *
 * <p>This is {@code VariableCatalog}'s <em>registered, never listed</em> applied to functions, and the
 * reasoning there is the reasoning here.
 *
 * <h2>⚠️ Only {@link AccessFunction}</h2>
 *
 * <p>The constructor takes the marker type and not {@link Function}, which is what stops
 * {@code jmouse-el}'s own {@code class(…)}, {@code set(…)} and reflection functions being swept in by a
 * bean container. See {@link AccessFunction} for what that would have cost.
 */
public final class FunctionCatalog {

    private static final FunctionCatalog EMPTY = new FunctionCatalog(List.of());

    private final Map<String, AccessFunction> byName;

    public FunctionCatalog(List<AccessFunction> contributed) {
        Map<String, AccessFunction> index = new LinkedHashMap<>();

        for (AccessFunction function : contributed) {
            requireName(function);

            AccessFunction clashing = index.put(function.getName(), function);

            if (clashing != null) {
                throw new PolicyException(
                        "two functions are both called '%s' — %s and %s. A policy naming it would run "
                        + "whichever happened to be registered last, so the rule somebody read and the "
                        + "rule that ran could differ."
                                .formatted(function.getName(),
                                           clashing.getClass().getName(),
                                           function.getClass().getName()));
            }
        }

        this.byName = Map.copyOf(index);
    }

    /** What an installation contributing nothing has: a dialect with no functions, exactly as before. */
    public static FunctionCatalog empty() {
        return EMPTY;
    }

    public boolean contains(String name) {
        return byName.containsKey(name);
    }

    /**
     * The contribution itself, or null where nothing registers that name.
     *
     * <p>⚠️ Unwrapped, unlike {@link #functions()}. The load-time check asks a function about its own
     * arguments, which is a question for the contribution rather than for the failure-attributing
     * wrapper around it.
     */
    public AccessFunction find(String name) {
        return byName.get(name);
    }

    /** Every name, in a stable order — what a refusal lists and what an editor offers. */
    public Set<String> all() {
        return new TreeSet<>(byName.keySet());
    }

    /**
     * What {@link ConditionDialect} registers — each contribution wrapped so that its failures are
     * attributable.
     *
     * <p>⚠️ The wrapper is why a dead counter store does not silently lift every quota. A function that
     * throws would otherwise reach the condition as an anonymous {@code RuntimeException}, be read as
     * {@code false}, and — in a {@code deny} rule — mean <em>the deny does not apply</em>. Naming the
     * failure is what lets the axis apply the rule instead. See {@link ConditionFunctionFailure}.
     */
    public List<Function> functions() {
        return byName.values().stream().map(Attributed::new).map(Function.class::cast).toList();
    }

    /**
     * One contributed function, with its failures labelled.
     *
     * <p>⚠️ It deliberately does not catch {@link ConditionFunctionFailure} itself — a function calling
     * another one should not have its inner failure relabelled with the outer one's name.
     */
    private record Attributed(AccessFunction delegate) implements Function {

        @Override
        public Object execute(Arguments arguments, EvaluationContext context) {
            try {
                return delegate.execute(arguments, context);
            } catch (ConditionFunctionFailure alreadyAttributed) {
                throw alreadyAttributed;
            } catch (RuntimeException failed) {
                throw new ConditionFunctionFailure(delegate.getName(), delegate.failsOpen(), failed);
            }
        }

        @Override
        public String getName() {
            return delegate.getName();
        }
    }

    public boolean isEmpty() {
        return byName.isEmpty();
    }

    /**
     * ⚠️ Refused here rather than discovered later. A function with no name cannot be called, cannot be
     * checked for at load, and cannot be named in a refusal — it is a bean that silently does nothing,
     * which is worse than a missing one because nobody goes looking for it.
     */
    private static void requireName(AccessFunction function) {
        if (function.getName() == null || function.getName().isBlank()) {
            throw new PolicyException(
                    "the function contributed by %s has no name, so no condition could ever call it"
                            .formatted(function.getClass().getName()));
        }
    }
}
