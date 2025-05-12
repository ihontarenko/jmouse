package org.jmouse.el.extension.calculator.operation.handler;

import org.jmouse.el.extension.calculator.operation.IllegalOperationException;
import org.jmouse.el.extension.calculator.operation.OperationHandler;
import org.jmouse.el.extension.calculator.operation.OperationType;

import java.util.Collection;
import java.util.List;

import static org.jmouse.el.extension.calculator.operation.OperationType.MINUS;
import static org.jmouse.el.extension.calculator.operation.OperationType.PLUS;

/**
 * {@code OperationHandler} implementation for {@link Collection} types.
 * <p>
 * Supports the following operations:
 * <ul>
 *   <li>{@link OperationType#PLUS}    - adds an element or all elements of another collection</li>
 *   <li>{@link OperationType#MINUS}   - removes an element or all elements of another collection</li>
 * </ul>
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 *   Collection<String> list = new ArrayList<>(List.of("a", "b"));
 *   OperationHandler<Collection<?>>, Object> handler = new CollectionOperationHandler();
 *   handler.execute(OperationType.PLUS, list, "c");          // [a, b, c]
 *   handler.execute(OperationType.MINUS, list, List.of("a")); // [b, c]
 * }</pre>
 * </p>
 */
public class CollectionOperationHandler implements OperationHandler<Collection<?>, Object> {

    /**
     * Determines whether this handler supports the given operation and operand types.
     * <p>
     * This handler supports any operation on objects whose class is a subtype of {@link Collection}.
     * </p>
     *
     * @param type the operation type (PLUS or MINUS)
     * @param x    the class of the left operand (collection)
     * @param y    the class of the right operand (any object or collection)
     * @return {@code true} if {@code x} is a {@link Collection} subtype; {@code false} otherwise
     */
    @Override
    public boolean supports(OperationType type, Class<?> x, Class<?> y) {
        return x != null && Collection.class.isAssignableFrom(x);
    }

    /**
     * Executes the collection operation.
     * <p>
     * For {@link OperationType#PLUS}, adds the supplied value:
     * <ul>
     *   <li>If {@code value} is a collection, all its elements are added.</li>
     *   <li>Otherwise, the single {@code value} is added.</li>
     * </ul>
     * For {@link OperationType#MINUS}, removes the supplied value:
     * <ul>
     *   <li>If {@code value} is a collection, all its elements are removed.</li>
     *   <li>Otherwise, the single {@code value} is removed.</li>
     * </ul>
     * </p>
     *
     * @param type      the operation type (PLUS or MINUS)
     * @param object    the collection to modify
     * @param value     the element or collection of elements to add or remove
     * @return the modified collection
     * @throws IllegalOperationException if attempted operation is not PLUS or MINUS
     */
    @Override
    public Object execute(OperationType type, Collection<?> object, Object value) {
        @SuppressWarnings("unchecked")
        Collection<Object> collection = (Collection<Object>) object;

        return switch (type) {
            case PLUS -> {
                if (value instanceof Collection<?> source) {
                    collection.addAll(source);
                } else {
                    collection.add(value);
                }
                yield collection;
            }
            case MINUS -> {
                if (value instanceof Collection<?> source) {
                    collection.removeAll(source);
                } else {
                    collection.remove(value);
                }
                yield collection;
            }
            default -> throw new IllegalOperationException(
                    "Operator '%s' can't be applied to collections. Only %s can be applied."
                            .formatted(type, List.of(MINUS, PLUS)));
        };
    }
}
