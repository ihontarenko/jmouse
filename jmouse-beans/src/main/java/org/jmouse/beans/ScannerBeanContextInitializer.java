package org.jmouse.beans;

import org.jmouse.beans.annotation.Ignore;
import org.jmouse.beans.annotation.SuppressException;
import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.beans.definition.BeanDefinitionFactory;
import org.jmouse.beans.scanner.ConfigurationAnnotatedClassBeanScanner;
import org.jmouse.beans.scanner.ProvideAnnotatedClassesBeanScanner;
import org.jmouse.util.Priority;
import org.jmouse.util.helper.Arrays;
import org.slf4j.Logger;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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

        addScanner(new ProvideAnnotatedClassesBeanScanner());
        addScanner(new ConfigurationAnnotatedClassBeanScanner());
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

        Class<?>[]            rootClasses       = Arrays.concatenate(baseClasses, context.getBaseClasses());
        Class<?>[]            uniqueTypes       = Arrays.unique(rootClasses);
        int                   counter           = 0;
        BeanDefinitionFactory definitionFactory = context.getBeanDefinitionFactory();

        for (BeanScanner<AnnotatedElement> scanner : scanners) {
            SCAN:
            for (AnnotatedElement element : scanner.scan(uniqueTypes)) {
                if (ignoreElement(element)) {
                    LOGGER.warn("Ignoring candidate '{}'", element);
                    continue;
                }

                counter++;
                BeanDefinition beanDefinition = definitionFactory.createDefinition(element, context);
                try {
                    context.registerDefinition(beanDefinition);
                } catch (Exception exception) {
                    SuppressException annotation = beanDefinition.getAnnotation(SuppressException.class);

                    if (annotation != null) {
                        for (Class<? extends Throwable> throwableType : annotation.value()) {
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
     * Checks whether the given {@link AnnotatedElement} should be ignored based on the presence of the {@link Ignore} annotation.
     * <p>
     * The method evaluates:
     * <ul>
     *     <li>If the element itself is annotated with {@link Ignore}, it will be ignored.</li>
     *     <li>If the element is a {@link Method}, and its declaring class is annotated with {@link Ignore}, it will also be ignored.</li>
     * </ul>
     *
     * @param element the {@link AnnotatedElement} to check
     * @return {@code true} if the element should be ignored, {@code false} otherwise
     */
    private boolean ignoreElement(AnnotatedElement element) {
        boolean ignore = element.isAnnotationPresent(Ignore.class);

        if (!ignore && element instanceof Method method) {
            ignore = method.getDeclaringClass().isAnnotationPresent(Ignore.class);
        }

        return ignore;
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
