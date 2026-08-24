package org.jmouse.access.el.condition;

import org.jmouse.access.policy.PolicyException;
import org.jmouse.access.spi.ConditionFunctionFailure;
import org.jmouse.core.reflection.TypeClassifier;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Everything an installation lets a policy condition apply with {@code is} — and nothing else.
 *
 * <p>{@link FunctionCatalog}'s counterpart, built for the same three readers that must agree: the
 * compiler builds the dialect from it, {@link ConditionCalls} checks names against it at load, and a
 * policy editor offers names from it.
 *
 * <p>⚠️ The built-in tests {@link ConditionDialect} lists are <strong>not</strong> here. Those are chosen
 * by the dialect deliberately, one by one; this holds only what a product contributed.
 */
public final class TestCatalog {

    private static final TestCatalog EMPTY = new TestCatalog(List.of());

    private final Map<String, AccessTest> byName;

    public TestCatalog(List<AccessTest> contributed) {
        Map<String, AccessTest> index = new LinkedHashMap<>();

        for (AccessTest test : contributed) {
            requireName(test);

            AccessTest clashing = index.put(test.getName(), test);

            if (clashing != null) {
                throw new PolicyException(
                        ("two tests are both called '%s' — %s and %s. A rule naming it would apply "
                         + "whichever happened to be registered last, so the rule somebody read and the "
                         + "rule that ran could differ.")
                                .formatted(test.getName(),
                                           clashing.getClass().getName(),
                                           test.getClass().getName()));
            }
        }

        this.byName = Map.copyOf(index);
    }

    public static TestCatalog empty() {
        return EMPTY;
    }

    public boolean contains(String name) {
        return byName.containsKey(name);
    }

    public AccessTest find(String name) {
        return byName.get(name);
    }

    public Set<String> all() {
        return new TreeSet<>(byName.keySet());
    }

    public boolean isEmpty() {
        return byName.isEmpty();
    }

    /**
     * The contributed tests, each wrapped so a throw carries who threw it and what to do about it.
     */
    public List<Test> tests() {
        return byName.values().stream().map(Attributed::new).map(Test.class::cast).toList();
    }

    /**
     * ⚠️ The wrapper is the fail-closed guarantee, not a tidiness.
     *
     * <p>{@code ExpressionCondition.holds} reads <em>any</em> escaping exception as {@code false}. In a
     * conditional allow that refuses, which is safe; in a conditional <strong>deny</strong> a {@code false}
     * means the deny does not hold — so a test that threw would <em>permit</em>, silently.
     *
     * <p>Turning it into a {@link ConditionFunctionFailure} carries it past that {@code catch} to
     * {@code ConditionAxis}, the first place that knows which of the two it is evaluating: it applies the
     * deny and drops the allow. Both refuse.
     *
     * <p>A test is the shape most likely to be reached for inside a {@code deny}, which is exactly why
     * this could not be left to symmetry.
     */
    private record Attributed(AccessTest delegate) implements Test {

        @Override
        public boolean test(
                Object value, Arguments arguments, EvaluationContext context, TypeClassifier type) {

            try {
                return delegate.test(value, arguments, context, type);
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

    private static void requireName(AccessTest test) {
        if (test.getName() == null || test.getName().isBlank()) {
            throw new PolicyException(
                    "the test contributed by %s has no name, so no condition could ever apply it"
                            .formatted(test.getClass().getName()));
        }
    }
}
