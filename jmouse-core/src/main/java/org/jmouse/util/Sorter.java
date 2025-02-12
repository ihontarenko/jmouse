package org.jmouse.util;

import java.util.Comparator;
import java.util.List;

import static java.util.Objects.requireNonNullElse;
import static org.jmouse.core.reflection.Reflections.getAnnotationValue;

/**
 * A utility class for sorting objects according to a priority scheme. The priority
 * may be obtained from an annotation (i.e., {@code @Priority}) on a class or from
 * the {@link Ordered} interface if the bean implements it. If no priority information
 * is available, objects are sorted to the end (i.e., {@code Integer.MAX_VALUE}),
 * and ties are further resolved by comparing hash codes.
 *
 * @see Priority
 * @see Ordered
 */
public final class Sorter {

    /**
     * A comparator that first compares objects by priority (annotation or
     * {@link Ordered#getOrder()}) and then, if priorities are equal, by their hash codes.
     */
    public static final PriorityComparator PRIORITY_COMPARATOR = new PriorityComparator();

    /**
     * Sorts the given list of objects by priority, then by hash code if priorities
     * are equal.
     *
     * @param collection the list of objects to be sorted
     * @param <T>        the type of objects in the list
     */
    public static <T> void sort(List<T> collection) {
        collection.sort(PRIORITY_COMPARATOR.thenComparing(Object::hashCode));
    }

    /**
     * A comparator that compares objects based on their priority. Priority is obtained
     * by one of the following means (in order):
     * <ol>
     *   <li>Checking if the bean is a {@link Class} annotated with {@code @Priority}.</li>
     *   <li>Checking if the bean implements {@link Ordered}.</li>
     *   <li>Otherwise, assigning {@code Integer.MAX_VALUE} as the priority.</li>
     * </ol>
     *
     * <p>Higher-priority objects are considered "less" for sorting purposes (i.e., sorted first).
     */
    public static final class PriorityComparator implements Comparator<Object> {

        @Override
        public int compare(Object a, Object b) {
            return Integer.compare(extractor(a), extractor(b));
        }

        /**
         * Extracts the priority of the given bean.
         *
         * @param object the bean whose priority will be extracted
         * @return the priority value, or {@code Integer.MAX_VALUE} if
         *         no priority could be determined
         */
        public static int extractor(Object object) {
            int order = Integer.MIN_VALUE;

            if (object != null) {
                if (object instanceof Class<?> klass) {
                    order = getAnnotationValue(klass, Priority.class, Priority::value);
                } else if (object instanceof Ordered ordered) {
                    order = ordered.getOrder();
                } else {
                    Integer priority = getAnnotationValue(object.getClass(), Priority.class, Priority::value);
                    order = requireNonNullElse(priority, order);
                }
            }

            return order;
        }

    }

}
