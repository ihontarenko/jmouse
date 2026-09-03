package org.jmouse.validator.constraint.constraint;

import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.api.ConstraintExecutor;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

/**
 * How much of something there may be — characters, elements, entries. 📏
 *
 * <p>Counts a {@link String} by its characters, a {@link Collection} or {@link Map} by its entries, and
 * an array by its length. Anything else has no size, and a constraint asked for the size of something
 * that has none fails rather than guessing one.</p>
 *
 * <h3>Behaviour</h3>
 * <ul>
 *     <li>{@code null} is valid — compose with {@code required} when presence is the question.</li>
 *     <li>A boundary left unset is not enforced, so {@code min} alone is a floor with no ceiling.</li>
 * </ul>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * @Size('min':3,'max':32,'message':'Three to thirty-two characters')
 * }</pre>
 *
 * <p>⚠️ <strong>This class was a stub until 2026-08-27.</strong> It computed a size and then returned
 * {@code false} unconditionally, and was registered nowhere — so the whole string half of the constraint
 * set was missing, and Innoventa's field editor shipped invented examples ({@code length(value) >= 3})
 * for checks that did not exist to be named.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class SizeConstraint implements Constraint {

    private Integer min;
    private Integer max;
    private String  message;

    /**
     * Returns a stable constraint code.
     *
     * @return {@code "size"}
     */
    @Override
    public String code() {
        return "size";
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public Object[] arguments() {
        return new Object[]{min, max};
    }

    @Override
    public ConstraintExecutor<SizeConstraint> executor() {
        return Executor.INSTANCE;
    }

    /**
     * Counts the value, or reports that it cannot be counted.
     *
     * <p>⚠️ Returns a boxed {@link Integer} rather than {@code -1} for "no size": a sentinel and a real
     * length share a type, and the day something legitimately measures {@code -1} the two become
     * indistinguishable.</p>
     *
     * @param value the value being validated
     * @return how many, or {@code null} when the value has no size
     */
    private static Integer sizeOf(Object value) {
        return switch (value) {
            case CharSequence characters -> characters.length();
            case Collection<?> collection -> collection.size();
            case Map<?, ?> map -> map.size();
            default -> value.getClass().isArray() ? Array.getLength(value) : null;
        };
    }

    private static final class Executor implements ConstraintExecutor<SizeConstraint> {

        private static final Executor INSTANCE = new Executor();

        @Override
        public boolean test(Object value, SizeConstraint constraint) {
            if (value == null) {
                return true;
            }

            Integer size = sizeOf(value);

            if (size == null) {
                return false;
            }

            if (constraint.min != null && size < constraint.min) {
                return false;
            }

            return constraint.max == null || size <= constraint.max;
        }
    }

    /**
     * @return the smallest size accepted, or {@code null} when there is no floor
     */
    public Integer getMin() {
        return min;
    }

    /**
     * Sets the smallest size accepted.
     */
    public void setMin(Integer min) {
        this.min = min;
    }

    /**
     * @return the largest size accepted, or {@code null} when there is no ceiling
     */
    public Integer getMax() {
        return max;
    }

    /**
     * Sets the largest size accepted.
     */
    public void setMax(Integer max) {
        this.max = max;
    }

    /**
     * @return custom validation message (may be {@code null})
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets custom validation message.
     */
    public void setMessage(String message) {
        this.message = message;
    }
}
