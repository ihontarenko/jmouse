package org.jmouse.validator.el.runtime;

import java.util.List;
import java.util.Map;

/**
 * What one check written in a {@code .jmv} means in terms of a constraint. 🧾
 *
 * <h2>⚠️ This is where {@code size(3, 32)} stops being two anonymous numbers</h2>
 *
 * <p>A constraint binds its arguments <strong>by property name</strong> — {@code min}, {@code max},
 * {@code regex}, {@code allowed}. A check writes them <strong>by position</strong>, because
 * {@code size(3, 32)} is what somebody wants to type. Something has to say which position is which
 * name, and it cannot be the constraint: a Java bean knows its properties and has no opinion about
 * their order.</p>
 *
 * <p>⚠️ <strong>And it is the same table the form-builder draws from.</strong> A builder showing
 * {@code size} as two number inputs needs exactly this — how many there are, and what to label them.
 * Positional names are therefore a <em>public</em> fact about the language, not an implementation
 * detail of its compiler.</p>
 *
 * @param check      the word written in the file — {@code size}, {@code min}, {@code oneOf}
 * @param constraint the constraint it builds, as {@code ConstraintTypeRegistry} knows it
 * @param fixed      properties the check sets by existing at all — {@code min} is {@code MinMax} with
 *                   {@code mode} already decided, which is the whole difference between them
 * @param positional the property each positional argument fills, in order
 * @param variadic   whether the last positional name collects every remaining argument into a list,
 *                   which is what makes {@code oneOf('SMD', 'THT')} readable
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record CheckSignature(
        String              check,
        String              constraint,
        Map<String, Object> fixed,
        List<String>        positional,
        boolean             variadic
) {

    public CheckSignature {
        fixed = Map.copyOf(fixed);
        positional = List.copyOf(positional);
    }

    /**
     * A check taking arguments by position.
     *
     * @param check      the word written in the file
     * @param constraint the constraint it builds
     * @param positional the property each argument fills, in order
     * @return the signature
     */
    public static CheckSignature of(String check, String constraint, String... positional) {
        return new CheckSignature(check, constraint, Map.of(), List.of(positional), false);
    }

    /**
     * A check taking any number of arguments, collected into one property.
     *
     * @param check      the word written in the file
     * @param constraint the constraint it builds
     * @param collecting the property every argument goes into
     * @return the signature
     */
    public static CheckSignature collecting(String check, String constraint, String collecting) {
        return new CheckSignature(check, constraint, Map.of(), List.of(collecting), true);
    }

    /**
     * The same check, with a property it always sets.
     *
     * @param key   the property
     * @param value what it is always set to
     * @return a new signature; this one is unchanged
     */
    public CheckSignature with(String key, Object value) {
        java.util.Map<String, Object> merged = new java.util.LinkedHashMap<>(fixed);

        merged.put(key, value);

        return new CheckSignature(check, constraint, merged, positional, variadic);
    }

    /**
     * How many positional arguments this check accepts at most.
     *
     * @return the count, or {@link Integer#MAX_VALUE} when it collects
     */
    public int arity() {
        return variadic ? Integer.MAX_VALUE : positional.size();
    }
}
