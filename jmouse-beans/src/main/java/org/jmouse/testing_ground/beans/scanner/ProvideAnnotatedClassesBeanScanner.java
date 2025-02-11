package org.jmouse.testing_ground.beans.scanner;

import org.jmouse.testing_ground.beans.BeanScanner;
import org.jmouse.testing_ground.beans.annotation.Provide;
import org.jmouse.core.reflection.ClassFinder;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A scanner that identifies classes or elements annotated with {@link Provide}.
 */
public class ProvideAnnotatedClassesBeanScanner implements BeanScanner<AnnotatedElement> {

    /**
     * Scans for classes or elements annotated with {@link Provide}.
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
