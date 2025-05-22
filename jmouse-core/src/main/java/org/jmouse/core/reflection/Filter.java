package org.jmouse.core.reflection;

import org.jmouse.core.matcher.Matcher;

import java.lang.reflect.Member;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Function;

/**
 * Interface for filtering reflection members in a class.
 *
 * The {@code Filter} interface provides a contract for filtering reflection members (such as methods, fields, or constructors)
 * based on specific matching criteria. It allows clients to query a class for members that meet the filtering conditions.
 * The filter also provides access to the current {@link Matcher} used for filtering.
 *
 * <p>Example usage:
 * <pre>{@code
 *     Filter<Method> filter = new MethodFilter(support, matcher, SomeClass.class);
 *     List<Method> methods = filter.find();
 *     Matcher<Method> currentMatcher = filter.matcher();
 * }</pre>
 *
 * @param <T> the type of {@link Member} (e.g., {@link java.lang.reflect.Method}, {@link java.lang.reflect.Field})
 */
public interface Filter<T extends Member> {

    /**
     * Finds all members of the specified type that match the filter criteria.
     *
     * @return a list of matching members
     */
    Collection<T> find();

    /**
     * Gets the current matcher being used to filter the members.
     *
     * @return the {@link Matcher} used to filter the members
     */
    Matcher<T> matcher();

    Filter<T> by(Matcher<T> matcher, Function<Matcher<T>, Matcher<T>> logical);

    default Filter<T> by(Matcher<T> matcher) {
        return by(matcher, Matcher::logicalAnd);
    }

    Filter<T> sortBy(Comparator<T> comparator);

}
