package org.jmouse.el.extension.calculator.operation.handler;

import org.jmouse.el.extension.calculator.operation.IllegalOperationException;
import org.jmouse.el.extension.calculator.operation.OperationHandler;
import org.jmouse.el.extension.calculator.operation.OperationType;
import org.jmouse.util.helper.Arrays;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jmouse.el.extension.calculator.operation.OperationType.MINUS;
import static org.jmouse.el.extension.calculator.operation.OperationType.PLUS;

/**
 * 🧮 Handles arithmetic-like operations for Java arrays.
 * Supports dynamic addition and removal of elements.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
public class ArrayOperationHandler implements OperationHandler<Object[], Object> {

    /**
     * ✅ Checks if the left operand is an array.
     *
     * @param type operation type
     * @param x    left operand class
     * @param y    right operand class
     * @return true if {@code x} is array
     */
    @Override
    public boolean supports(OperationType type, Class<?> x, Class<?> y) {
        return x != null && x.isArray();
    }

    /**
     * 🚀 Performs operation on arrays:
     * <ul>
     *     <li>{@code +} adds {@code object} to the end</li>
     *     <li>{@code -} removes all matching occurrences</li>
     * </ul>
     *
     * @param type   operation type (only PLUS and MINUS supported)
     * @param array  source array
     * @param object operand to add or remove
     * @return modified array (new instance)
     * @throws IllegalOperationException for unsupported operations
     */
    @Override
    public Object execute(OperationType type, Object[] array, Object object) {
        return switch (type) {
            case INCREMENT -> {
                int      oldSize  = array.length;
                int      newSize  = oldSize + 1;

                yield Arrays.expand(array, newSize);
            }
            case DECREMENT -> {
                int      oldSize  = array.length;
                int      newSize  = oldSize - 1;

                if (newSize <= 0) {
                    yield new Object[0];
                }

                yield Arrays.expand(array, newSize);
            }
            case PLUS -> {
                int      oldSize  = array.length;
                int      newSize  = oldSize + 1;
                Object[] newArray = Arrays.expand(array, newSize); // 🔧 Expands array

                newArray[oldSize] = object; // ➕ Add element to the end

                yield newArray;
            }
            case MINUS -> {
                List<Object> newArray = new ArrayList<>(array.length);
                Collections.addAll(newArray, array);

                newArray.removeIf(v -> v.equals(object));

                yield newArray.toArray(); // 🎁 Return new array
            }
            default -> throw new IllegalOperationException(
                    "Operator '%s' can't be applied to arrays. Only %s are supported.".formatted(type, List.of(MINUS, PLUS)));
        };
    }

}
