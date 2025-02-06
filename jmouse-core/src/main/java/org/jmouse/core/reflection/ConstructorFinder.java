package org.jmouse.core.reflection;

import org.jmouse.core.matcher.Matcher;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A class that finds constructors in a given class. It supports scanning superclasses
 * to retrieve inherited constructors if required.
 */
public class ConstructorFinder implements MemberFinder<Constructor<?>> {

    /**
     * Retrieves all constructors from the specified class.
     *
     * @param clazz the class whose constructors are to be retrieved
     * @param deepScan whether to scan superclasses for members
     * @return a collection of constructors from the class
     */
    @Override
    public Collection<Constructor<?>> getMembers(Class<?> clazz, boolean deepScan) {
        Set<Constructor<?>> constructors = new HashSet<>(Set.of(clazz.getDeclaredConstructors()));

        // Optionally scan superclasses to include their constructors
        if (deepScan && clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
            constructors.addAll(getMembers(clazz.getSuperclass(), true));
        }

        return constructors;
    }

    /**
     * Returns a {@link ConstructorFilter} to apply additional filtering criteria to constructors of the specified class.
     *
     * @param clazz the class to filter constructors from
     * @return a {@link ConstructorFilter} instance for filtering constructors
     */
    @Override
    public ConstructorFilter filter(Class<?> clazz) {
        return new ConstructorFilter(this, Matcher.constant(true), clazz);
    }

}
