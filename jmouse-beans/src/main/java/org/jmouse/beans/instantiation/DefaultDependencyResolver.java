package org.jmouse.beans.instantiation;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanInstantiationException;
import org.jmouse.beans.definition.AggregatedBeansDependency;
import org.jmouse.beans.definition.BeanDependency;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.ClassMatchers;
import org.jmouse.core.reflection.JavaType;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Default implementation of {@link DependencyResolver}, resolving single beans by type and name,
 * or aggregations of beans into collections when requested.
 * <p>
 * - If the dependency is an {@link AggregatedBeansDependency}, fetches all matching beans by the raw type,
 * and returns either a {@link Set} or {@link Collection} based on the target type.
 * - Otherwise, delegates to {@link BeanContext#getBean(Class, String)} for a single bean lookup.
 * </p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class DefaultDependencyResolver implements DependencyResolver {

    private static final Matcher<Class<?>> IS_SET  = ClassMatchers.isSubtype(Set.class);
    private static final Matcher<Class<?>> IS_LIST = ClassMatchers.isSubtype(List.class);

    /**
     * {@inheritDoc}
     * <p>
     * For {@link AggregatedBeansDependency}, retrieves all beans of the specified type,
     * converting to a {@link Set} if the dependency target is a Set. For single-bean dependencies,
     * calls {@link BeanContext#getBean(Class, String)}.
     * </p>
     *
     * @param dependency the bean dependency descriptor
     * @param context    the bean context used for retrieval
     * @return a single bean instance or a collection of beans matching the dependency
     */
    @Override
    public Object resolve(BeanDependency dependency, BeanContext context) {
        Object resolved;

        if (dependency instanceof AggregatedBeansDependency(JavaType javaType, String name, Object dependant)) {
            // Fetch all beans of the declared raw type
            Collection<Object> beans = context.getBeans(getAggregatedType(javaType));

            // If the target type is a Set, wrap in HashSet to enforce uniqueness
            if (IS_SET.matches(javaType.getRawType())) {
                resolved = Set.copyOf(beans);
            } else if (IS_LIST.matches(javaType.getRawType())) {
                resolved = List.copyOf(beans);
            } else {
                throw new BeanInstantiationException(
                        "Unable resolve aggregated dependency. Dependant: (%s)".formatted(dependant));
            }
        } else {
            // Single-bean lookup by type and name
            resolved = context.getBean(dependency.type(), dependency.name());
        }

        return resolved;
    }

    private Class<Object> getAggregatedType(JavaType javaType) {
        return javaType.getFirst().getRawType();
    }

}
