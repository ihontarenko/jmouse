package org.jmouse.beans;

import org.jmouse.beans.annotation.SuppressException;
import org.jmouse.beans.definition.BeanDefinitionFactory;
import org.jmouse.util.Priority;
import org.jmouse.util.Sorter;
import org.jmouse.util.helper.Arrays;
import org.slf4j.Logger;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;

import static org.jmouse.core.reflection.Reflections.getAnnotationValue;
import static org.jmouse.core.reflection.Reflections.getShortName;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * This class is responsible for initializing a {@link BeanContext} by scanning
 * specified base classes for annotated elements using registered {@link BeanScanner}s.
 *
 * <p>Usage binder:</p>
 * <pre>{@code
 * BeanContext context = new BeanContext();
 * ScannerBeanContextInitializer initializer = new ScannerBeanContextInitializer(new Class<?>[]{ RootClass.class });
 * initializer.addScanner(new BeanAnnotatedClassesScanner());
 * initializer.initialize(context);
 * }</pre>
 */
@Priority(-1000)
public class ScannerBeanContextInitializer implements BeanContextInitializer {

    private static final Logger LOGGER = getLogger(getShortName(BeanContextInitializer.class) + ".SCANNER");

    private final Class<?>[]                          baseClasses;
    private final List<BeanScanner<AnnotatedElement>> scanners = new ArrayList<>();

    /**
     * Creates a new instance of {@code ScannerBeanContextInitializer}.
     *
     * @param baseClasses the base classes to scan for annotated elements.
     */
    public ScannerBeanContextInitializer(Class<?>... baseClasses) {
        this.baseClasses = baseClasses;
    }

    /**
     * Initializes the provided {@link BeanContext} by scanning the base classes
     * and registering bean definitions for the annotated elements.
     *
     * @param context the {@link BeanContext} to initialize.
     */
    @Override
    public void initialize(BeanContext context) {
        LOGGER.info("{} scanners ready for run", scanners.size());

        List<BeanScanner<AnnotatedElement>> scanners = this.scanners;

        Sorter.sort(scanners);

        int                   counter           = 0;
        BeanDefinitionFactory definitionFactory = context.getBeanDefinitionFactory();
        Class<?>[]            baseClasses       = Arrays.concatenate(this.baseClasses, context.getBaseClasses());

        for (BeanScanner<AnnotatedElement> scanner : scanners) {
            SCAN:
            for (AnnotatedElement element : scanner.scan(baseClasses)) {
                counter++;

                try {
                    context.registerDefinition(definitionFactory.createDefinition(element, context));
                } catch (Exception exception) {
                    Class<? extends Throwable>[] throwableTypes = getAnnotationValue(
                            element, SuppressException.class, SuppressException::value);

                    if (throwableTypes != null) {
                        for (Class<? extends Throwable> throwableType : throwableTypes) {
                            if (throwableType.isAssignableFrom(exception.getClass())) {
                                LOGGER.warn("Suppressed exception '{}'", exception.getMessage());
                                continue SCAN;
                            }
                        }
                    }

                    throw exception;
                }
            }
        }

        LOGGER.info("{} annotated elements were handled", counter);
    }

    /**
     * Adds a {@link BeanScanner} to this initializer.
     *
     * @param beanScanner the {@link BeanScanner} to add.
     */
    public void addScanner(BeanScanner<AnnotatedElement> beanScanner) {
        this.scanners.add(beanScanner);
    }

    /**
     * Removes all registered scanners from this initializer.
     */
    public void clearScanner() {
        this.scanners.clear();
    }

}
