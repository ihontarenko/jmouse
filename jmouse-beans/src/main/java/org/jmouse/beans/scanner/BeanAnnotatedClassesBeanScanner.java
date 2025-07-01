package org.jmouse.beans.scanner;

import org.jmouse.beans.BeanScanner;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.core.reflection.ClassFinder;
import org.jmouse.util.Priority;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A scanner that identifies classes or elements annotated with {@link Bean}.
 */
@Priority(Integer.MIN_VALUE)
public class BeanAnnotatedClassesBeanScanner implements BeanScanner<AnnotatedElement> {

    /**
     * Scans for classes or elements annotated with {@link Bean}.
     *
     * @param baseClasses the base classes to scan for annotated elements.
     * @return a collection of {@link AnnotatedElement} objects (annotated classes or implementations).
     */
    @Override
    public Collection<AnnotatedElement> scan(Class<?>... baseClasses) {
        List<AnnotatedElement> elements = new ArrayList<>();

        for (Class<?> annotatedClass : ClassFinder.findAnnotatedClasses(Bean.class, baseClasses)) {
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
