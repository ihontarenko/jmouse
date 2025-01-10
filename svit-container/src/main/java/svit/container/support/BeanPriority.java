package svit.container.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Comparator;

/**
 * Indicates a priority value for beans or bean-producing methods.
 * <p>
 * This annotation can be used to order bean registration or discovery,
 * ensuring certain beans are processed earlier or later based on the provided {@code value}.
 * <p>
 * Example usage:
 * <pre>{@code
 * @BeanPriority(1)
 * public class HighPriorityBean {
 *     // ...
 * }
 *
 * @BeanPriority(10)
 * public class LowPriorityBean {
 *     // ...
 * }
 *
 * // Sorting example:
 * List<Class<?>> beanClasses = List.of(HighPriorityBean.class, LowPriorityBean.class);
 * beanClasses.sort(new BeanPriority.BeanPriorityComparator());
 * // The list is now sorted in ascending order based on @BeanPriority.value
 * }</pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface BeanPriority {

    /**
     * The priority value for the annotated type or method.
     * A lower value indicates higher priority.
     *
     * @return the priority value.
     */
    int value();


    /**
     * A comparator for classes annotated with {@link BeanPriority}.
     * It compares two classes based on their {@link BeanPriority#value()}.
     *
     * <p>If both classes are missing the annotation, or if both have
     * the same priority value, they will be considered equal.</p>
     */
    final class BeanPriorityComparator implements Comparator<Class<?>> {

        @Override
        public int compare(Class<?> classA, Class<?> classB) {
            BeanPriority annotationA = classA.getAnnotation(BeanPriority.class);
            BeanPriority annotationB = classB.getAnnotation(BeanPriority.class);

            // If both classes have the annotation, compare their values
            if (annotationA != null && annotationB != null) {
                return Integer.compare(annotationA.value(), annotationB.value());
            }

            // If only one of them has the annotation, decide how you want to order them
            // For example, classes without annotation could be considered the lowest priority:
            if (annotationA == null && annotationB == null) {
                return 0;
            }

            // classA has no annotation, classB does => classA goes after classB and vise versa
            return annotationA == null ? 1 : -1;
        }

    }

}
