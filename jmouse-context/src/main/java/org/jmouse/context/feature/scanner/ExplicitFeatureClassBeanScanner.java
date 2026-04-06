package org.jmouse.context.feature.scanner;

import org.jmouse.beans.BeanScanner;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.BeanImport;
import org.jmouse.beans.scanner.BeanFactoriesAnnotatedClassBeanScanner;
import org.jmouse.beans.scanner.BeanImportAnnotatedClassBeanScanner;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ExplicitFeatureClassBeanScanner implements BeanScanner<AnnotatedElement> {

    private static final BeanFactoriesAnnotatedClassBeanScanner FACTORY_SCANNER = new BeanFactoriesAnnotatedClassBeanScanner();
    private static final BeanImportAnnotatedClassBeanScanner    IMPORTS_SCANNER = new BeanImportAnnotatedClassBeanScanner();

    @Override
    public Collection<AnnotatedElement> scan(Class<?>... classes) {
        List<AnnotatedElement> elements = new ArrayList<>();

        if (classes == null) {
            return elements;
        }

        for (Class<?> annotatedElement : classes) {
            if (annotatedElement == null) {
                continue;
            }

            if (annotatedElement.isAnnotationPresent(BeanImport.class)) {
                elements.addAll(IMPORTS_SCANNER.getElements(annotatedElement));
                continue;
            }

            if (annotatedElement.isAnnotationPresent(BeanFactories.class)) {
                elements.addAll(FACTORY_SCANNER.getElements(annotatedElement));
                continue;
            }

            if (annotatedElement.isAnnotationPresent(Bean.class)) {
                elements.add(annotatedElement);
            }
        }

        return Set.copyOf(elements);
    }
}