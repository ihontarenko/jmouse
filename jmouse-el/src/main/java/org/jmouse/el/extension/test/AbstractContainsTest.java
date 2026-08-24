package org.jmouse.el.extension.test;

import org.jmouse.core.support.ArraySupport;
import org.jmouse.el.extension.Arguments;

import java.util.Collection;
import java.util.List;

abstract public class AbstractContainsTest extends AbstractTest {

    protected Collection<?> toExpected(Arguments arguments) {
        return toExpected(arguments, null);
    }

    /**
     * What the caller is looking for, as a collection.
     *
     * <h2>⚠️ A single string argument is only taken apart when the VALUE is a string too</h2>
     *
     * <p>Unwrapping {@code 'abc'} into its characters is right for {@code 'abc' is contains('b')} — that
     * is what asking whether text contains text means. It is <strong>wrong</strong> the moment the value
     * is a collection:</p>
     *
     * <pre>
     *   tags is contains('smd')   →  false     ← 'smd' became ['s','m','d'], which ['smd','0805'] lacks
     *   'smd' in tags             →  true      ← the same question, answered correctly
     * </pre>
     *
     * <p>⚠️ Two ways of asking one thing gave two answers, and neither raised. The fix is not to stop
     * unwrapping — that behaviour is documented and used — but to unwrap only where it means something,
     * which needs the value the test is about.</p>
     *
     * @param arguments what was passed to the test
     * @param value     what the test is about, or {@code null} where a caller cannot say
     * @return the expected elements
     */
    protected Collection<?> toExpected(Arguments arguments, Object value) {
        if (arguments.isEmpty()) {
            return List.of();
        }

        Object first = arguments.getFirst();

        if (arguments.size() == 1) {
            if (first instanceof Collection<?> collection) {
                return collection;
            }
            if (ArraySupport.isArray(first)) {
                return ArraySupport.toList(first);
            }
            if (first instanceof CharSequence charSequence && isText(value)) {
                return ArraySupport.toCodePoints(charSequence.toString());
            }
        }

        return arguments.toList();
    }

    /**
     * Whether the value being tested is itself text.
     *
     * <p>⚠️ {@code null} means "the caller did not say", and that is treated as text — the historical
     * behaviour. A caller that knows better passes the value.</p>
     */
    private boolean isText(Object value) {
        return value == null || value instanceof CharSequence;
    }

}
