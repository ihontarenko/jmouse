package org.jmouse.core.reflection;

import svit.support.context.AbstractContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A specialized context for managing root classes during scanning operations.
 * Extends {@link AbstractContext} to provide additional functionality specific to root class management.
 *
 * <p>Example usage:
 * <pre>{@code
 * ScannerContext context = new ScannerContext();
 * context.addDefaultRootClass(MyRootClass.class);
 * List<Class<?>> rootClasses = context.getDefaultRootClasses();
 * System.out.println(rootClasses); // Outputs: [class MyRootClass]
 * }</pre>
 */
public final class ScannerContext extends AbstractContext {

    /**
     * Retrieves the default root classes associated with {@link ClassFinder}.
     *
     * @return a list of default root classes.
     */
    public List<Class<?>> getDefaultRootClasses() {
        return getRootClasses(ClassFinder.class);
    }

    /**
     * Retrieves the root classes associated with a specific caller.
     *
     * @param caller the class representing the caller.
     * @return a list of root classes for the given caller.
     */
    public List<Class<?>> getRootClasses(Class<?> caller) {
        return getProperty(caller, new ArrayList<>());
    }

    /**
     * Adds a root class to the list associated with a specific caller.
     *
     * @param caller    the class representing the caller.
     * @param rootClass the root class to add.
     */
    public void addRootClass(Class<?> caller, Class<?> rootClass) {
        List<Class<?>> classes = getRootClasses(caller);
        classes.add(rootClass);
        setProperty(caller, classes);
    }

    /**
     * Adds a default root class to the list associated with {@link ClassFinder}.
     *
     * @param rootClass the default root class to add.
     */
    public void addDefaultRootClass(Class<?> rootClass) {
        addRootClass(ClassFinder.class, rootClass);
    }

    /**
     * Cleans up (clears) the root classes associated with a specific caller.
     *
     * @param caller the class representing the caller.
     */
    public void cleanupClasses(Class<?> caller) {
        setProperty(caller, new ArrayList<>());
    }
}