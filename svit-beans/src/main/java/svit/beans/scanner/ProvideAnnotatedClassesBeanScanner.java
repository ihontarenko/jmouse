package svit.beans.scanner;

import svit.beans.BeanContext;
import svit.beans.BeanScanner;
import svit.beans.annotation.Provide;
import svit.reflection.ClassFinder;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A scanner that identifies classes or elements annotated with {@link Provide}.
 * <p>
 * This implementation scans the specified base classes and their subclasses
 * to identify all classes annotated with {@link Provide}. For annotated interfaces,
 * it also scans for their implementations within the provided base classes.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ProvideAnnotatedClassesBeanScanner scanner = new ProvideAnnotatedClassesBeanScanner();
 * Collection<AnnotatedElement> elements = scanner.scan(AppConfig.class);
 * elements.forEach(System.out::println);
 * }</pre>
 */
public class ProvideAnnotatedClassesBeanScanner implements BeanScanner<AnnotatedElement> {

    /**
     * Scans for classes or elements annotated with {@link Provide}.
     * <p>
     * For each base class provided, this method:
     * <ul>
     *   <li>Identifies all classes annotated with {@link Provide}.</li>
     *   <li>For annotated interfaces, finds all their implementations within the given base classes.</li>
     *   <li>Adds annotated classes directly if they are not interfaces.</li>
     * </ul>
     *
     * @param baseClasses the base classes to scan for annotated elements.
     * @return a collection of {@link AnnotatedElement} objects (annotated classes or implementations).
     */
    @Override
    public Collection<AnnotatedElement> scan(Class<?>... baseClasses) {
        List<AnnotatedElement> elements = new ArrayList<>();

        for (Class<?> annotatedClass : ClassFinder.findAnnotatedClasses(Provide.class, baseClasses)) {
            if (annotatedClass.isInterface()) {
                elements.addAll(ClassFinder.findImplementations(annotatedClass, baseClasses));
            } else {
                // Add the annotated class directly
                elements.add(annotatedClass);
            }
        }

        return elements;
    }
}
