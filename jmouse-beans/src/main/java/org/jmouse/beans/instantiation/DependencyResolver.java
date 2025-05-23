package org.jmouse.beans.instantiation;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.definition.BeanDependency;

/**
 * Strategy interface for resolving a {@link BeanDependency} into an actual bean instance
 * within a given {@link BeanContext}.
 * <p>
 * Implementations determine how to retrieve or construct the bean(s) required by the dependency,
 * for example by name, type, or as a collection of matching beans.
 * </p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface DependencyResolver {

    /**
     * Resolve the specified dependency against the provided context.
     *
     * @param dependency the descriptor of the bean(s) to resolve
     * @param context    the context from which beans are retrieved
     * @return the resolved bean instance, a collection of beans, or null if not found
     */
    Object resolve(BeanDependency dependency, BeanContext context);

    /**
     * Interface to be implemented by beans that need access to the current {@link DependencyResolver}.
     * <p>
     * Allows setting and retrieving the resolver used for nested or dynamic dependencies.
     * </p>
     */
    interface Aware {

        /**
         * Inject the resolver to be used by this bean for dependency lookup.
         *
         * @param resolver the dependency resolver to set
         */
        void setDependencyResolver(DependencyResolver resolver);

        /**
         * Obtain the resolver currently set on this bean.
         *
         * @return the active {@link DependencyResolver}
         */
        DependencyResolver getDependencyResolver();

    }

}
