package svit.beans.scanner;

import svit.beans.BeanScanner;
import svit.beans.annotation.Configuration;
import svit.beans.annotation.Provide;
import svit.reflection.ClassFinder;
import svit.reflection.MethodFinder;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A {@link BeanScanner} implementation that scans for methods annotated with {@link Provide}
 * in classes annotated with {@link Configuration}.
 *
 * <p>This scanner uses the {@link ClassFinder} to locate classes annotated with
 * {@link Configuration} and the {@link MethodFinder} to filter and find methods
 * annotated with {@link Provide} within those classes.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ConfigurationAnnotatedClassBeanScanner scanner = new ConfigurationAnnotatedClassBeanScanner();
 * Collection<AnnotatedElement> elements = scanner.scan(MyAppConfig.class);
 * elements.forEach(element -> System.out.println("Found: " + element));
 * }</pre>
 */
public class ConfigurationAnnotatedClassBeanScanner implements BeanScanner<AnnotatedElement> {

    /**
     * Scans the specified base classes for classes annotated with {@link Configuration}.
     * Finds and returns methods annotated with {@link Provide} within those classes.
     *
     * @param baseClasses the base classes to scan for annotated elements.
     * @return a collection of annotated elements found in the scanned classes.
     */
    @Override
    public Collection<AnnotatedElement> scan(Class<?>... baseClasses) {
        List<AnnotatedElement> elements = new ArrayList<>();

        // Find classes annotated with @Configuration
        for (Class<?> klass : ClassFinder.findAnnotatedClasses(Configuration.class, baseClasses)) {
            // Find methods annotated with @Provide in each @Configuration class
            elements.addAll(new MethodFinder().filter(klass).annotated(Provide.class).find());
        }

        return elements;
    }
}
