package org.jmouse.core.reflection;

import org.jmouse.core.matcher.Matcher;

import java.lang.reflect.Method;
import java.util.*;

/**
 * A class that finds methods in a given class. It supports scanning superclasses
 * and interfaces to retrieve inherited or implemented methods.
 */
public class MethodFinder implements MemberFinder<Method> {

    public static final MethodFinder FINDER = new MethodFinder();

    /**
     * Retrieves all methods from the specified class.
     *
     * @param clazz the class whose methods are to be retrieved
     * @param deepScan whether to scan superclasses for methods
     * @return a collection of methods from the class
     */
    @Override
    public Collection<Method> getMembers(Class<?> clazz, boolean deepScan) {
        Set<Method> methods = new HashSet<>(Set.of(clazz.getDeclaredMethods()));

        // Optionally scan superclasses for methods (except Object.class)
        if (deepScan && clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
            methods.addAll(getMembers(clazz.getSuperclass(), true));
        }

        // Optionally scan interfaces for methods
        if (deepScan) {
            for (Class<?> ifc : clazz.getInterfaces()) {
                methods.addAll(getMembers(ifc, true));
            }
        }

        return methods;
    }

    /**
     * Retrieves all methods from the specified class which satisfy the condition according to the parameters types
     *
     * @param clazz the class whose methods are to be retrieved
     * @return a collection of methods from the class
     */
    public static Collection<Method> getMethods(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        return FINDER.filter(clazz).parameterTypes(parameterTypes).methodName(methodName).find();
    }

    /**
     * Returns a {@link MethodFilter} to apply additional filtering criteria to methods of the specified class.
     *
     * @param clazz the class to filter members from
     * @return a {@link Filter} instance for filtering members
     */
    @Override
    public MethodFilter filter(Class<?> clazz) {
        return new MethodFilter(this, Matcher.constant(true), clazz);
    }

}
