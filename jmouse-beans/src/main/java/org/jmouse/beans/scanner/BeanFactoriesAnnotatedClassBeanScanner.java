package org.jmouse.beans.scanner;

import org.jmouse.beans.BeanScanner;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.core.reflection.ClassFinder;
import org.jmouse.core.reflection.MethodFinder;
import org.jmouse.core.Priority;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A {@link BeanScanner} implementation that scans for methods annotated with {@link Bean}
 * in classes annotated with {@link BeanFactories}.
 *
 * <p>This scanner uses the {@link ClassFinder} to locate classes annotated with
 * {@link BeanFactories} and the {@link MethodFinder} to filter and find methods
 * annotated with {@link Bean} within those classes.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * BeanFactoriesAnnotatedClassBeanScanner scanner = new BeanFactoriesAnnotatedClassBeanScanner();
 * Collection<AnnotatedElement> elements = scanner.scan(MyAppConfig.class);
 * elements.forEach(element -> System.out.println("Found: " + element));
 * }</pre>
 */
@Priority(Integer.MIN_VALUE + 1)
public class BeanFactoriesAnnotatedClassBeanScanner implements BeanScanner<AnnotatedElement> {

    /**
     * Scans the specified base classes for classes annotated with {@link BeanFactories}.
     * Finds and returns methods annotated with {@link Bean} within those classes.
     *
     * @param baseClasses the base classes to scan for annotated elements.
     * @return a collection of annotated elements found in the scanned classes.
     */
    @Override
    public Collection<AnnotatedElement> scan(Class<?>... baseClasses) {
        List<AnnotatedElement> elements = new ArrayList<>();

        // Find classes annotated with @Factories
        for (Class<?> klass : ClassFinder.findAnnotatedClasses(BeanFactories.class, baseClasses)) {
            elements.addAll(getElements(klass));
        }

        return elements;
    }

    /**
     * 📦 Extracts all bean-related elements from the given factory source.
     *
     * <p>This method collects:</p>
     * <ul>
     *     <li>the factory class itself (annotated with {@link BeanFactories})</li>
     *     <li>all methods annotated with {@link Bean} declared within that class</li>
     * </ul>
     *
     * <p>Effectively, it represents a single "bean factory unit" — combining
     * the configuration class and its factory methods into a unified set
     * of {@link AnnotatedElement}s for further processing.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * @BeanFactories
     * class AppConfig {
     *
     *     @Bean
     *     Service service() { ... }
     * }
     *
     * // Result:
     * // - AppConfig.class
     * // - Method: service()
     * }</pre>
     *
     * @param element the factory class (expected to be annotated with {@link BeanFactories})
     * @return collection containing the class and its {@link Bean}-annotated methods
     */
    public Collection<AnnotatedElement> getElements(AnnotatedElement element) {
        List<AnnotatedElement> elements = new ArrayList<>();

        // Add factory @Factories annotated class
        elements.add(element);
        // Find methods annotated with @Bean in each @Configuration class
        elements.addAll(new MethodFinder().filter((Class<?>) element).annotated(Bean.class).find());

        return elements;
    }

}
