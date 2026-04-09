package org.jmouse.beans;

import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.List;

/**
 * 🔍 Contract for scanning classes and extracting annotated elements.
 *
 * <p>
 * Implementations are responsible for inspecting provided base classes
 * and returning a collection of {@link AnnotatedElement elements}
 * (e.g. methods, fields, constructors, or classes themselves).
 * </p>
 *
 * <p>
 * This abstraction is a core extension point in the bean discovery pipeline,
 * allowing different scanning strategies such as:
 * </p>
 * <ul>
 *     <li>annotation-based scanning (e.g. {@code @Bean}, {@code @Configuration})</li>
 *     <li>explicit class processing</li>
 *     <li>recursive import resolution</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * BeanScanner<Method> scanner = new MethodAnnotationScanner();
 * Collection<Method> methods = scanner.scan(AppConfig.class);
 * methods.forEach(method -> System.out.println(method.getName()));
 * }</pre>
 *
 * @param <T> type of {@link AnnotatedElement} produced by this scanner
 */
public interface BeanScanner<T extends AnnotatedElement> {

    /**
     * 📦 Scans the given base classes and extracts annotated elements.
     *
     * <p>
     * Implementations may perform:
     * </p>
     * <ul>
     *     <li>direct inspection of provided classes</li>
     *     <li>recursive traversal (e.g. imports, meta-annotations)</li>
     *     <li>filtering based on annotations or other criteria</li>
     * </ul>
     *
     * @param baseClasses classes to scan
     * @return collection of discovered elements (never {@code null})
     */
    Collection<T> scan(Class<?>... baseClasses);

    /**
     * 🔧 Hook for processing a single {@link AnnotatedElement}.
     *
     * <p>
     * Allows scanners to delegate element-specific handling logic
     * (e.g. extracting nested elements, applying transformations, etc).
     * </p>
     *
     * <p>
     * Default implementation returns an empty collection.
     * </p>
     *
     * @param element element to process
     * @return additional derived elements, or empty collection
     */
    default Collection<AnnotatedElement> handleAnnotatedElement(AnnotatedElement element) {
        return List.of();
    }

}