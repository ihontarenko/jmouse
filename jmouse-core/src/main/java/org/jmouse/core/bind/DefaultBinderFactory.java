package org.jmouse.core.bind;

import org.jmouse.core.Sorter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A default implementation of {@link BinderFactory}.
 * <p>
 * This factory maintains a list of registered {@link ObjectBinder} instances and provides
 * a mechanism for retrieving the appropriate binder based on the provided {@link TypedValue} type.
 * Binders are sorted before being evaluated to ensure priority-based selection.
 * </p>
 *
 * @author JMouse - Team
 * @author Mr. Jerry Mouse
 * @author Ivan Hontarenko
 */
public class DefaultBinderFactory implements BinderFactory {

    private final List<ObjectBinder> binders = new ArrayList<>();

    /**
     * Retrieves the most suitable {@link ObjectBinder} for the given {@link TypedValue} type.
     * <p>
     * The registered binders are sorted before context to ensure priority-based selection.
     * If no matching binder is found, a {@link BindException} is thrown.
     * </p>
     *
     * @param bindable the bindable type to find a binder for
     * @return the appropriate {@link ObjectBinder} instance
     * @throws BindException if no suitable binder is found
     */
    @Override
    public ObjectBinder getBinderFor(TypedValue<?> bindable) {
        Sorter.sort(this.binders);

        for (ObjectBinder binder : binders) {
            if (binder.supports(bindable)) {
                return binder;
            }
        }

        throw new BindException("No binder found for bindable type '%s'.".formatted(bindable.getType()));
    }

    /**
     * Registers a new {@link ObjectBinder} to the factory.
     *
     * @param binder the binder instance to register
     */
    @Override
    public void registerBinder(ObjectBinder binder) {
        this.binders.add(binder);
    }

    /**
     * Retrieves all registered {@link ObjectBinder} instances.
     *
     * @return a collection of registered binders
     */
    @Override
    public Collection<ObjectBinder> getBinders() {
        return this.binders;
    }

    /**
     * Unregisters a previously registered {@link ObjectBinder}.
     *
     * @param binder the binder instance to remove
     */
    @Override
    public void unregisterBinder(ObjectBinder binder) {
        this.binders.remove(binder);
    }

    /**
     * Clears all registered {@link ObjectBinder} instances.
     */
    @Override
    public void clearBinders() {
        this.binders.clear();
    }
}
