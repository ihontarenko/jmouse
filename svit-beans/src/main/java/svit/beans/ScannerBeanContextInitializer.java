package svit.beans;

import org.slf4j.Logger;
import svit.beans.definition.BeanDefinition;
import svit.beans.definition.BeanDefinitionFactory;
import svit.beans.scanner.ConfigurationAnnotatedClassBeanScanner;
import svit.beans.scanner.ProvideAnnotatedClassesBeanScanner;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;
import static svit.reflection.Reflections.getShortName;

/**
 * This class is responsible for initializing a {@link BeanContext} by scanning
 * specified base classes for annotated elements using registered {@link BeanScanner}s.
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * BeanContext context = new BeanContext();
 * ScannerBeanContextInitializer initializer = new ScannerBeanContextInitializer(new Class<?>[]{ RootClass.class });
 * initializer.addScanner(new BeanAnnotatedClassesScanner());
 * initializer.initialize(context);
 * }</pre>
 */
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

        int                   counter           = 0;
        BeanDefinitionFactory definitionFactory = context.getBeanDefinitionFactory();

        for (BeanScanner<AnnotatedElement> scanner : scanners) {
            for (AnnotatedElement annotatedElement : scanner.scan(baseClasses)) {
                counter++;
                BeanDefinition beanDefinition = definitionFactory.createDefinition(annotatedElement, context);
                context.registerDefinition(beanDefinition);
            }
        }

        LOGGER.info("{} annotated elements were found", counter);
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
    public void removeScanner() {
        this.scanners.clear();
    }

}
