package org.jmouse.util;

import java.util.Comparator;

/**
 * Utility class for comparing arbitrary objects with null safety.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
public final class AnyComparator {

    private AnyComparator() {
        throw new UnsupportedOperationException();
    }

    /**
     * Compares two objects safely.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static int compare(Object left, Object right) {
        if (left == right) {
            return 0;
        }

        if (left == null) {
            return -1;
        }

        if (right == null) {
            return 1;
        }

        if (left instanceof Comparable a && right instanceof Comparable b) {
            if (a.getClass().equals(b.getClass())) {
                return a.compareTo(b);
            }
        }

        return Integer.compare(System.identityHashCode(left), System.identityHashCode(right));
    }

    /**
     * Returns a comparator instance for use in collections.
     *
     * @param <T> the type to compare
     * @return a comparator instance
     */
    public static <T> Comparator<T> comparator() {
        return AnyComparator::compare;
    }
}
