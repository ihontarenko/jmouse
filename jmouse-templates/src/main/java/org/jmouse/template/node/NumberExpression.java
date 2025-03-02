package org.jmouse.template.node;

/**
 * Represents a numeric expression in a templating system.
 *
 * <p>This node holds a numeric value, which can be used in calculations or comparisons
 * within a template expression tree.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class NumberExpression extends AbstractExpression {

    /**
     * The numeric value stored in this expression.
     */
    private final Number number;

    /**
     * Constructs a {@code NumberExpression} with the specified numeric value.
     *
     * @param number the numeric value of this expression
     */
    public NumberExpression(Number number) {
        this.number = number;
    }

    /**
     * Returns the numeric value of this expression.
     *
     * @return the stored numeric value
     */
    public Number getNumber() {
        return number;
    }
}
