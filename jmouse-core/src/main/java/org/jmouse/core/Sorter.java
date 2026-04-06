package org.jmouse.core;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;

import static java.util.Objects.requireNonNullElse;
import static org.jmouse.core.reflection.Reflections.getAnnotationValue;

/**
 * A utility class for sorting objects according to a priority scheme. The priority
 * may be obtained from an annotation (i.e., {@code @Priority}) on a class or from
 * the {@link Ordered} interface if the structured implements it. If no priority information
 * is available, objects are sorted to the end (i.e., {@code Integer.MAX_VALUE}),
 * and ties are further resolved by comparing hash codes.
 *
 * @see Priority
 * @see Ordered
 */
public final class Sorter {

    /**
     * A comparator that first compares objects by priority (annotation or
     * {@link Ordered#order()}) and then, if priorities are equal, by their hash codes.
     */
    public static final PriorityComparator PRIORITY_COMPARATOR = new PriorityComparator();

    /**
     * A comparator that orders reflection elements by their kind.
     *
     * <p>The default order is:
     * <ol>
     *   <li>{@link Class}</li>
     *   <li>{@link Method}</li>
     *   <li>{@link Constructor}</li>
     *   <li>{@link Field}</li>
     *   <li>any other type</li>
     * </ol>
     */
    public static final ReflectionTypeComparator REFLECTION_TYPE_COMPARATOR = new ReflectionTypeComparator();

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
     * Sorts the given reflection elements by their runtime reflection type.
     *
     * <p>The sort order is defined by {@link ReflectionTypeComparator}.
     *
     * @param collection the reflection elements to sort
     * @param <T>        the reflection element type
     */
    public static <T extends AnnotatedElement> void sortReflection(List<T> collection) {
        collection.sort(
                REFLECTION_TYPE_COMPARATOR
                        .thenComparing(Object::toString)
        );
    }

    /**
     * A comparator that compares objects based on their priority. Priority is obtained
     * by one of the following means (in order):
     * <ol>
     *   <li>Checking if the structured is a {@link Class} annotated with {@code @Priority}.</li>
     *   <li>Checking if the structured implements {@link Ordered}.</li>
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
         * Extracts the priority of the given structured.
         *
         * @param object the structured whose priority will be extracted
         * @return the priority value, or {@code Integer.MAX_VALUE} if
         *         no priority could be determined
         */
        public static int extractor(Object object) {
            int order = Integer.MIN_VALUE;

            if (object != null) {
                if (object instanceof Class<?> klass) {
                    order = getAnnotationValue(klass, Priority.class, Priority::value);
                } else if (object instanceof Ordered ordered) {
                    order = ordered.order();
                } else {
                    Integer priority = getAnnotationValue(object.getClass(), Priority.class, Priority::value);
                    order = requireNonNullElse(priority, order);
                }
            }

            return order;
        }

    }

    /**
     * Compares reflection objects by their kind.
     *
     * <p>This comparator is useful when a collection contains mixed reflection elements
     * such as classes, methods, constructors, and fields, and they must be grouped
     * in a deterministic order before further processing.
     */
    public static final class ReflectionTypeComparator implements Comparator<Object> {

        @Override
        public int compare(Object a, Object b) {
            return Integer.compare(extractor(a), extractor(b));
        }

        /**
         * Extracts a numeric rank for the given reflection object.
         *
         * <p>Lower values are sorted first.
         *
         * @param object the object to inspect
         * @return the reflection-type rank
         */
        public static int extractor(Object object) {
            if (object instanceof Class<?>) {
                return 0;
            }
            if (object instanceof Method) {
                return 1;
            }
            if (object instanceof Constructor<?>) {
                return 2;
            }
            if (object instanceof Field) {
                return 3;
            }
            return Integer.MAX_VALUE;
        }

    }

}