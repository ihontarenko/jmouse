package svit.util;

/**
 * A functional interface representing an entity with an order or priority.
 * Implementations can specify their relative order by overriding the {@link #getOrder()} method.
 */
@FunctionalInterface
public interface Ordered {

    /**
     * Returns the order or priority of this object.
     * Lower values typically represent higher priority.
     *
     * @return the order value
     */
    int getOrder();
}
