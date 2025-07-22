package org.jmouse.core.reflection;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.scanner.ClassScanner;
import org.jmouse.core.reflection.scanner.DefaultClassScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.*;

import static org.jmouse.core.reflection.ClassMatchers.*;

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
 * <p>This binder demonstrates how to find classes annotated with a specific annotation
 * and sort them using predefined and custom comparators.</p>
 */
public interface ClassFinder {

    Logger LOGGER = LoggerFactory.getLogger(ClassFinder.class);

    /**
     * A cache for storing previously scanned and filtered classes.
     * Key: hash of base classes and matcher used in the search.
     * Value: a collection of matched classes.
     */
    Map<CacheKey, Collection<Class<?>>> CACHE = new HashMap<>();

    /**
     * The default class scanner used for locating classes in the classpath.
     */
    ClassScanner SCANNER = new DefaultClassScanner();

    /**
     * The scanner context containing default configuration for scanning classes.
     */
    ScannerContext CONTEXT = new ScannerContext();

    /**
     * A comparator for sorting classes alphabetically by their names.
     */
    Comparator<Class<?>> ORDER_CLASS_NAME = Comparator.comparing(Class::getName);

    /**
     * A comparator for no ordering.
     */
    Comparator<Class<?>> NO_ORDERING = (o1, o2) -> 0;

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
     * Finds all implementations of a given interface.
     *
     * @param superClass the superclass to look for
     * @param baseClasses the base classes to scan
     * @return a collection of implementations
     */
    static Collection<Class<?>> findInheritedClasses(Class<?> superClass, Class<?>... baseClasses) {
        return findAll(isSubtype(superClass).and(isAbstract().not()), baseClasses);
    }

    /**
     * Finds all classes matching the given matcher.
     *
     * @param matcher the matcher to filter classes
     * @param baseClasses the base classes to scan
     * @return a collection of matching classes
     */
    static Collection<Class<?>> findAll(Matcher<Class<?>> matcher, Class<?>... baseClasses) {
        return findAll(matcher, NO_ORDERING, baseClasses);
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
     * 🔍 Scans and filters classes from base packages using the given matcher and optional comparators.
     *
     * <p>⚡ Supports caching and lazy evaluation. If no base classes are provided,
     * the context's default roots are used. Results can be sorted using provided comparators.</p>
     *
     * @param matcher     a matcher to filter scanned classes
     * @param comparators optional comparators for sorting results
     * @param baseClasses root classes to scan from (optional)
     * @return collection of matched and optionally sorted classes
     */
    static Collection<Class<?>> findAll(
            Matcher<Class<?>> matcher, Collection<Comparator<Class<?>>> comparators, Class<?>... baseClasses) {
        // Retrieve base classes from context if none are passed
        if (baseClasses == null || baseClasses.length == 0) {
            baseClasses = CONTEXT.getDefaultRootClasses().toArray(Class<?>[]::new);
        }

        // Build a unique cache key based on base classes and matcher
        final CacheKey cacheKey = CacheKey.of(baseClasses);

        // Check cache
        Collection<Class<?>> classes = CACHE.get(cacheKey);

        if (classes == null) {
            classes = new LinkedHashSet<>();

            for (Class<?> baseClass : baseClasses) {
                classes.addAll(SCANNER.scan(baseClass.getClassLoader(), baseClass));
            }

            CACHE.put(cacheKey, classes);
        }

        // Combine comparators; default to no sorting if no comparators provided
        Comparator<Class<?>> comparator = comparators.stream()
                .reduce(Comparator::thenComparing)
                .orElse((a, b) -> 0);

        // Filter and sort classes
        List<Class<?>> result = classes.stream().filter(matcher::matches).sorted(comparator).toList();

        LOGGER.info("📦 Scanned: {} → Matched: {}; Matcher: {}", classes.size(), result.size(), matcher);

        return result;
    }

    /**
     * Retrieves the scanner context.
     *
     * @return the scanner context
     */
    static ScannerContext getContext() {
        return CONTEXT;
    }

    class CacheKey {

        private final Class<?>[] classes;

        public CacheKey(Class<?>[] classes) {
            this.classes = classes;
        }

        @Override
        public String toString() {
            return "KEY:[%s]".formatted(Arrays.toString(classes));
        }

        @Override
        public boolean equals(Object object) {
            if (object instanceof CacheKey cacheKey) {
                return Objects.deepEquals(classes, cacheKey.classes);
            }

            return false;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(classes);
        }

        public static CacheKey of(Class<?>... classes) {
            return new CacheKey(classes);
        }

    }

}
