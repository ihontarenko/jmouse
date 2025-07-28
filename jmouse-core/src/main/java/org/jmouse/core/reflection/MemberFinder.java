package org.jmouse.core.reflection;

import org.jmouse.core.matcher.Matcher;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;

/**
 * An interface for finding members (fields, methods, constructors) of a class that match specific criteria.
 *
 * <p>The {@code MemberFinder} interface provides methods to:
 * <ol>
 *     <li>Find members that match a {@link Matcher}.</li>
 *     <li>Sort the results using one or more {@link Comparator}s.</li>
 *     <li>Retrieve the first matching member.</li>
 *     <li>Filter members dynamically through the {@link Filter} abstraction.</li>
 * </ol>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Find all public methods in a class, sorted by name
 * Collection<Method> methods = memberFinder.find(
 *     MyClass.class,
 *     MemberMatcher.isPublic(),
 *     Comparator.comparing(Member::getName)
 * );
 *
 * // Find the first matching method
 * Optional<Method> firstMethod = memberFinder.findFirst(
 *     MyClass.class,
 *     MemberMatcher.isAnnotatedWith(MyAnnotation.class),
 *     List.of(Comparator.comparing(Member::getName))
 * );
 *
 * // Use dynamic filtering for members
 * Filter<Member> filter = memberFinder.filter(MyClass.class);
 * filter.by(MemberMatcher.isStatic()).sortedBy(Comparator.comparing(Member::getName)).findAll();
 * }</pre>
 *
 * @param <T> the type of {@link Member} being searched (e.g., {@link java.lang.reflect.Constructor}, {@link java.lang.reflect.Method}, {@link java.lang.reflect.Field})
 */
public interface MemberFinder<T extends Member> {

    Comparator<Executable> PARAMETERS_COUNT_CMP = Comparator.comparingInt(Executable::getParameterCount);

    static <T extends Member> Comparator<T> defaultComparator() {
        return Comparator.comparing(Member::getName);
    }

    /**
     * Returns a collection of members from the specified class.
     * This method should be implemented by subclasses to define which members are to be searched
     * (e.g., methods, fields, constructors).
     *
     * @param clazz the class whose members should be retrieved
     * @return a collection of members from the class
     */
    default Collection<T> getMembers(Class<?> clazz) {
        return getMembers(clazz, true);
    }

    /**
     * Returns a collection of members from the specified class.
     * This method should be implemented by subclasses to define which members are to be searched
     * (e.g., methods, fields, constructors).
     *
     * @param clazz the class whose members should be retrieved
     * @param deepScan whether to scan superclasses for members
     * @return a collection of members from the class
     */
    Collection<T> getMembers(Class<?> clazz, boolean deepScan);

    /**
     * Finds all members of the specified class that match the given {@link Matcher}.
     * The results are sorted using the default comparator by member name.
     *
     * <p>This method is a shortcut for calling {@link #find(Class, Matcher, Comparator)}
     * with {@code Comparator.comparing(Member::getName)} as the comparator.</p>
     *
     * @param clazz   the class whose members are to be searched
     * @param matcher the matcher to filter the members
     * @return a collection of members that match the criteria, sorted by name
     */
    default Collection<T> find(Class<?> clazz, Matcher<? super T> matcher) {
        return find(clazz, matcher, defaultComparator());
    }

    /**
     * Finds all members of the specified class that match the given {@link Matcher}.
     * The results are sorted using the provided comparator.
     *
     * <p>This method is a shortcut for calling {@link #find(Class, Matcher, Collection)}
     * with a single comparator in a list.</p>
     *
     * @param clazz      the class whose members are to be searched
     * @param matcher    the matcher to filter the members
     * @param comparator the comparator to sort the matched members
     * @return a collection of members that match the criteria, sorted as specified
     */
    default Collection<T> find(Class<?> clazz, Matcher<? super T> matcher, Comparator<? super T> comparator) {
        return find(clazz, matcher, Collections.singletonList(comparator));
    }

    /**
     * Finds all members of the specified class that match the given {@link Matcher}.
     * The results are sorted using the provided collection of {@link Comparator}s.
     *
     * <p>This method applies a strict matching mode first, ensuring the member belongs
     * to the declaring class. If no matches are found, it relaxes the condition
     * and applies the matcher to all members of the class.</p>
     *
     * @param clazz       the class whose members are to be searched
     * @param matcher     the matcher to filter the members
     * @param comparators a collection of comparators to sort the matched members; if empty, no sorting is applied
     * @return a collection of members that match the criteria, sorted as specified
     */
    @SuppressWarnings({"unchecked"})
    default Collection<T> find(Class<?> clazz, Matcher<? super T> matcher, Collection<Comparator<? super T>> comparators) {
        Matcher<Member> strictMatcher = MemberMatcher.isDeclaredClass(clazz).and((Matcher<? super Member>) matcher);
        Collection<T>   members       = getMembers(clazz);

        Collection<T> matched = members.stream()
                .filter(strictMatcher::matches).toList();

        if (matched.isEmpty()) {
            matched = members.stream().filter(matcher::matches).toList();
        }

        Comparator<T> comparator = comparators.stream()
                .map(cmp -> (Comparator<T>)cmp)
                .reduce(Comparator::thenComparing)
                .orElse((a, b) -> 0);

        return matched.stream().sorted(comparator).toList();
    }

    /**
     * Finds the first member of the specified class that matches the given {@link Matcher}.
     * The results are sorted using the provided collection of comparators before retrieving the first match.
     *
     * @param clazz       the class whose members are to be searched
     * @param matcher     the matcher to filter the members
     * @param comparators a collection of comparators for sorting the members
     * @return an {@link Optional} containing the first matching member, or empty if no match is found
     */
    default Optional<T> findFirst(Class<?> clazz, Matcher<? super T> matcher, Collection<Comparator<? super T>> comparators) {
        return find(clazz, matcher, comparators).stream().findFirst();
    }

    /**
     * Finds the first member of the specified class that matches the given {@link Matcher}.
     * The results are sorted using the provided comparator before retrieving the first match.
     *
     * <p>This method is a shortcut for calling {@link #findFirst(Class, Matcher, Collection)}
     * with a single comparator in a list.</p>
     *
     * @param clazz      the class whose members are to be searched
     * @param matcher    the matcher to filter the members
     * @param comparator the comparator to sort the members
     * @return an {@link Optional} containing the first matching member, or empty if no match is found
     */
    default Optional<T> findFirst(Class<?> clazz, Matcher<? super T> matcher, Comparator<? super T> comparator) {
        return findFirst(clazz, matcher, Collections.singletonList(comparator));
    }

    /**
     * Finds the first member of the specified class that matches the given {@link Matcher}.
     * The results are sorted by member name before retrieving the first match.
     *
     * <p>This method is a shortcut for calling {@link #findFirst(Class, Matcher, Comparator)}
     * with {@code Comparator.comparing(Member::getName)} as the comparator.</p>
     *
     * @param clazz   the class whose members are to be searched
     * @param matcher the matcher to filter the members
     * @return an {@link Optional} containing the first matching member, or empty if no match is found
     */
    default Optional<T> findFirst(Class<?> clazz, Matcher<? super T> matcher) {
        return findFirst(clazz, matcher, Comparator.comparing(Member::getName));
    }

    /**
     * Creates a {@link Filter} for dynamically filtering members of the specified class.
     * The filter allows chaining of matching and sorting operations to retrieve members on demand.
     *
     * @param clazz the class whose members are to be filtered
     * @return a {@link Filter} structured for the specified class
     */
    Filter<T> filter(Class<?> clazz);
}
