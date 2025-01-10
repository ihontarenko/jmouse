package svit.reflection;

import svit.scanner.ClassScanner;
import svit.matcher.Matcher;

import java.lang.annotation.Annotation;
import java.util.*;

import static svit.reflection.ClassMatchers.implementsInterface;
import static svit.reflection.ClassMatchers.isAbstract;

/**
 * ClassFinder provides utilities for finding, filtering, and sorting classes.
 * It includes caching mechanisms, a default class scanner, and a variety of
 * comparators to customize the sorting of classes.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Collection<Class<?>> result = ClassFinder.findAll(
 *     ClassMatchers.isAnnotatedWith(MyAnnotation.class), // Matcher to find annotated classes
 *     List.of(
 *         ClassFinder.ORDER_PACKAGE_NAME,                // Sort by package name
 *         ClassFinder.ORDER_MODIFIER,                    // Sort by modifiers
 *         Comparator.comparingInt(clazz
 *         -> clazz.getDeclaredConstructors().length)     // Custom comparator
 *     ),
 *     RootPackageClass.class                             // Base class for scanning
 * );
 * result.forEach(System.out::println);
 * }</pre>
 *
 * <p>This example demonstrates how to find classes annotated with a specific annotation
 * and sort them using predefined and custom comparators.</p>
 */
public interface ClassFinder {

    /**
     * A cache for storing previously scanned and filtered classes.
     * Key: hash of base classes and matcher used in the search.
     * Value: a collection of matched classes.
     */
    Map<Integer, Collection<Class<?>>> CACHE = new HashMap<>();

    /**
     * The default class scanner used for locating classes in the classpath.
     */
    ClassScanner SCANNER = ClassScanner.getDefaultScanner();

    /**
     * The scanner context containing default configuration for scanning classes.
     */
    ScannerContext CONTEXT = new ScannerContext();

    /**
     * A comparator for sorting classes alphabetically by their names.
     */
    Comparator<Class<?>> ORDER_CLASS_NAME = Comparator.comparing(Class::getName);

    /**
     * A comparator for sorting classes alphabetically by their simple-names.
     */
    Comparator<Class<?>> ORDER_CLASS_SIMPLE_NAME = Comparator.comparing(Class::getSimpleName);

    /**
     * A comparator for sorting classes alphabetically by their package names.
     */
    Comparator<Class<?>> ORDER_PACKAGE_NAME = Comparator.comparing(Class::getPackageName);

    /**
     * A comparator for sorting classes by their modifiers (e.g., public, abstract).
     * The modifiers are represented as integer values.
     */
    Comparator<Class<?>> ORDER_MODIFIER = Comparator.comparingInt(Class::getModifiers);

    /**
     * Finds all classes annotated with the given annotation.
     *
     * @param annotation the annotation to look for
     * @param baseClasses the base classes to scan
     * @return a collection of annotated classes
     */
    static Collection<Class<?>> findAnnotatedClasses(Class<? extends Annotation> annotation, Class<?>... baseClasses) {
        return findAll(ClassMatchers.isAnnotatedWith(annotation), baseClasses);
    }

    /**
     * Finds all enum classes.
     *
     * @param baseClasses the base classes to scan
     * @return a collection of enum classes
     */
    static Collection<Class<?>> findEnums(Class<?>... baseClasses) {
        return findAll(ClassMatchers.isEnum(), baseClasses);
    }

    /**
     * Finds all implementations of a given interface.
     *
     * @param interfaceClass the interface to look for
     * @param baseClasses the base classes to scan
     * @return a collection of implementations
     */
    static Collection<Class<?>> findImplementations(Class<?> interfaceClass, Class<?>... baseClasses) {
        return findAll(implementsInterface(interfaceClass).and(isAbstract().not()), baseClasses);
    }

    /**
     * Finds all classes matching the given matcher.
     *
     * @param matcher the matcher to filter classes
     * @param baseClasses the base classes to scan
     * @return a collection of matching classes
     */
    static Collection<Class<?>> findAll(Matcher<Class<?>> matcher, Class<?>... baseClasses) {
        return findAll(matcher, ORDER_CLASS_NAME, baseClasses);
    }

    /**
     * Finds all classes matching the given matcher and sorts them using the given comparator.
     *
     * @param matcher the matcher to filter classes
     * @param comparator the comparator to sort classes
     * @param baseClasses the base classes to scan
     * @return a sorted collection of matching classes
     */
    static Collection<Class<?>> findAll(
            Matcher<Class<?>> matcher, Comparator<Class<?>> comparator, Class<?>... baseClasses) {
        return findAll(matcher, Collections.singletonList(comparator), baseClasses);
    }

    /**
     * Finds all classes that match the given criteria, sorts them using the provided comparators,
     * and optionally scans the provided base classes' packages.
     *
     * <p>This method performs the following operations:</p>
     * <ul>
     *     <li>If no base classes are provided, it retrieves default root classes from the {@link ScannerContext}.</li>
     *     <li>Checks the {@link #CACHE} for already scanned and matched results. If found, returns the cached collection.</li>
     *     <li>Uses the {@link ClassScanner} to scan the packages of the base classes for all available classes.</li>
     *     <li>Applies the provided {@link Matcher} to filter the scanned classes.</li>
     *     <li>Sorts the filtered classes using the provided list of {@link Comparator} objects.
     *         If no comparators are provided, the original order is maintained.</li>
     * </ul>
     *
     * <p>Important notes:</p>
     * <ul>
     *     <li>The caching mechanism is based on a hash of the matcher and base classes to optimize performance.</li>
     *     <li>Sorting is optional, and the classes can be returned in their natural or scanned order if no comparators are provided.</li>
     *     <li>This method is thread-safe if the {@link #CACHE}, {@link ClassScanner}, and {@link ScannerContext} are accessed in a thread-safe manner.</li>
     * </ul>
     *
     * @param matcher     the matcher used to filter classes based on specific criteria; cannot be {@code null}.
     * @param comparators a list of comparators used to sort the filtered classes; can be empty to maintain original order.
     * @param baseClasses the base classes whose packages will be scanned; if empty, defaults from {@link ScannerContext} are used.
     * @return a collection of classes that match the given criteria, sorted as specified.
     * @throws IllegalArgumentException if the matcher is {@code null}.
     * @see Matcher
     * @see Comparator
     * @see ClassScanner
     * @see ScannerContext
     */
    static Collection<Class<?>> findAll(
            Matcher<Class<?>> matcher, Collection<Comparator<Class<?>>> comparators, Class<?>... baseClasses) {
        // Retrieve base classes from context if none are passed
        if (baseClasses == null || baseClasses.length == 0) {
            baseClasses = CONTEXT.getDefaultRootClasses().toArray(Class<?>[]::new);
        }

        // Build a unique cache key based on base classes and matcher
        final int cacheKey = Objects.hash(baseClasses);

        // Check cache
        Collection<Class<?>> classes = CACHE.get(cacheKey);

        if (classes == null) {
            classes = new HashSet<>();

            SCANNER.setMatcher(Matcher.constant(true));

            for (Class<?> baseClass : baseClasses) {
                classes.addAll(SCANNER.scan(baseClass.getPackageName(), baseClass.getClassLoader()));
            }

            CACHE.put(cacheKey, classes);
        }

        // Combine comparators; default to no sorting if no comparators provided
        Comparator<Class<?>> comparator = comparators.stream()
                .reduce(Comparator::thenComparing)
                .orElse((a, b) -> 0);

        // Filter and sort classes
        return classes.stream().filter(matcher::matches).sorted(comparator).toList();
    }

    /**
     * Retrieves the scanner context.
     *
     * @return the scanner context
     */
    static ScannerContext getContext() {
        return CONTEXT;
    }

}
