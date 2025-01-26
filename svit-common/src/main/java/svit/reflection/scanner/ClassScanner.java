package svit.reflection.scanner;

import svit.matcher.Matcher;

import java.util.Set;

/**
 * Interface for scanning classes based on specific criteria.
 * <p>
 * This interface provides methods to discover classes dynamically using matchers, class loaders, and base classes.
 * It supports both custom and default class loaders, as well as flexible matching conditions.
 * </p>
 */
public interface ClassScanner {

    /**
     * The default class loader used for scanning.
     */
    ClassLoader DEFAULT_CLASS_LOADER = ClassScanner.class.getClassLoader();

    /**
     * Scans for classes matching the given matcher, using the specified class loader and base classes.
     *
     * @param matcher    the matcher to filter classes
     * @param loader     the {@link ClassLoader} to use for scanning
     * @param baseClasses the base classes or interfaces to use as starting points for the scan
     * @return a set of classes that match the specified criteria
     */
    Set<Class<?>> scan(Matcher<Class<?>> matcher, ClassLoader loader, Class<?>... baseClasses);

    /**
     * Scans for classes matching the given matcher, using the default class loader and base classes.
     *
     * @param matcher    the matcher to filter classes
     * @param baseClasses the base classes or interfaces to use as starting points for the scan
     * @return a set of classes that match the specified criteria
     */
    default Set<Class<?>> scan(Matcher<Class<?>> matcher, Class<?>... baseClasses) {
        return scan(matcher, DEFAULT_CLASS_LOADER, baseClasses);
    }

    /**
     * Scans for classes using the specified class loader and base classes, with no filtering.
     *
     * @param loader      the {@link ClassLoader} to use for scanning
     * @param baseClasses the base classes or interfaces to use as starting points for the scan
     * @return a set of all classes found
     */
    default Set<Class<?>> scan(ClassLoader loader, Class<?>... baseClasses) {
        return scan(Matcher.constant(true), loader, baseClasses);
    }

    /**
     * Scans for classes using the default class loader and base classes, with no filtering.
     *
     * @param baseClasses the base classes or interfaces to use as starting points for the scan
     * @return a set of all classes found
     */
    default Set<Class<?>> scan(Class<?>... baseClasses) {
        return scan(DEFAULT_CLASS_LOADER, baseClasses);
    }
}
