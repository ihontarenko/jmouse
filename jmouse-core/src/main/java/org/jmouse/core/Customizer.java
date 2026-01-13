package org.jmouse.core;

import java.util.Objects;

/**
 * 🪝 Customizer — single-argument action.
 *
 * <p>A lightweight functional hook used to apply side effects to a given instance.
 * Conceptually equivalent to {@link java.util.function.Consumer Consumer&lt;T&gt;}.</p>
 *
 * @param <T> the input type being customized
 */
@FunctionalInterface
public interface Customizer<T> {

    /**
     * 🎯 Apply customization logic to the given instance.
     *
     * @param instance the target instance (may be {@code null} if implementation allows)
     */
    void customize(T instance);

    /**
     * 🔗 Combine multiple {@link Customizer} instances into a single one.
     *
     * <p>Execution order is preserved: the first element in {@code customizers}
     * runs first, then the next, and so on. If the array is {@code null} or empty,
     * the returned customizer is {@link #noop()}.</p>
     *
     * <p><b>Identity:</b> uses {@link #noop()} as the reduce identity and
     * composes each pair into a new customizer that invokes both in sequence.</p>
     *
     * <p><b>Note:</b> this implementation assumes that all entries in
     * {@code customizers} are non-{@code null}.</p>
     *
     * @param customizers one or more customizers to combine (execution order preserved)
     * @return a composed {@link Customizer} that applies all provided customizers in sequence
     * @implNote Implemented via {@code reduce(noop(), ...)} to avoid intermediate collections.
     * @see #noop()
     */
    @SuppressWarnings("unchecked")
    default Customizer<T> combine(Customizer<T>... customizers) {
        if (customizers == null || customizers.length == 0) {
            return noop();
        }

        return (instance) -> Streamable.of(customizers).filter(Objects::nonNull).reduce(noop(), (a, b) -> (x) -> {
            a.customize(x);
            b.customize(x);
        });
    }

    /**
     * 💤 No-op customizer — does nothing.
     *
     * @param <T> input type
     * @return a customizer that ignores its input
     */
    static <T> Customizer<T> noop() {
        return t -> {};
    }
}
