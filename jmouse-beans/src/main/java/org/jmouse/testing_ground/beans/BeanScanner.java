package org.jmouse.testing_ground.beans;

import java.lang.reflect.AnnotatedElement;
import java.util.Collection;

/**
 * Interface for scanning classes to find annotated elements.
 *
 * <p>Implementations of this interface are responsible for scanning the provided
 * base classes and returning a collection of annotated elements of the specified type.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * BeanScanner<Method> methodScanner = new MethodAnnotationScanner();
 * Collection<Method> methods = methodScanner.scan(BaseClass.class, BaseClass2.class);
 * methods.forEach(method -> System.out.println("Found method: " + method.getName()));
 * }</pre>
 *
 * @param <T> the type of annotated elements to scan (e.g., {@link java.lang.reflect.Method},
 *            {@link java.lang.reflect.Field}, {@link java.lang.reflect.Constructor}).
 */
public interface BeanScanner<T extends AnnotatedElement> {

    /**
     * Scans the specified base classes and returns a collection of annotated elements.
     *
     * @param baseClasses the classes to scan for annotated elements.
     * @return a collection of annotated elements of the specified type.
     */
    Collection<T> scan(Class<?>... baseClasses);

}
