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

/**
 * 📦 {@link BeanScanner} that processes explicitly provided feature classes.
 *
 * <p>
 * Unlike package-wide or annotation-discovery scanners, this scanner works only
 * with the classes passed directly into {@link #scan(Class[])}.
 * </p>
 *
 * <p>
 * Supported inputs:
 * </p>
 * <ul>
 *     <li>{@link BeanImport} classes — resolved through {@link BeanImportAnnotatedClassBeanScanner}</li>
 *     <li>{@link BeanFactories} classes — resolved through {@link BeanFactoriesAnnotatedClassBeanScanner}</li>
 *     <li>{@link Bean} classes — added directly as bean elements</li>
 * </ul>
 *
 * <p>
 * The resulting collection is deduplicated before being returned.
 * </p>
 */
public class ExplicitFeatureClassBeanScanner implements BeanScanner<AnnotatedElement> {

    /**
     * Scanner used for classes annotated with {@link BeanFactories}.
     */
    private static final BeanFactoriesAnnotatedClassBeanScanner FACTORY_SCANNER = new BeanFactoriesAnnotatedClassBeanScanner();

    /**
     * Scanner used for classes annotated with {@link BeanImport}.
     */
    private static final BeanImportAnnotatedClassBeanScanner IMPORTS_SCANNER = new BeanImportAnnotatedClassBeanScanner();

    /**
     * Scans the explicitly provided classes and extracts bean-relevant elements.
     *
     * <p>
     * Result order is not guaranteed after deduplication because
     * {@link Set#copyOf(Collection)} is used for the returned collection.
     * </p>
     *
     * @param classes explicit classes to inspect
     * @return deduplicated collection of discovered {@link AnnotatedElement}s
     */
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
            elements.addAll(handleAnnotatedElement(annotatedElement));
        }

        return Set.copyOf(elements);
    }

    @Override
    public Collection<AnnotatedElement> handleAnnotatedElement(AnnotatedElement element) {
        List<AnnotatedElement> elements = new ArrayList<>();

        if (element.isAnnotationPresent(BeanImport.class)) {
            elements.addAll(IMPORTS_SCANNER.handleAnnotatedElement(element));
        }

        if (element.isAnnotationPresent(BeanFactories.class)) {
            elements.addAll(FACTORY_SCANNER.handleAnnotatedElement(element));
        }

        if (element.isAnnotationPresent(Bean.class)) {
            elements.add(element);
        }

        return elements;
    }
}