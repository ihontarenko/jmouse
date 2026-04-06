package org.jmouse.context;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanScanner;
import org.jmouse.beans.annotation.SuppressException;
import org.jmouse.beans.definition.BeanDefinitionFactory;
import org.jmouse.core.Sorter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jmouse.core.reflection.Reflections.getAnnotationValue;

/**
 * Imports feature classes into a {@link BeanContext} by scanning them and registering
 * discovered bean definitions. 🧩
 *
 * <p>
 * A feature importer bridges two stages:
 * </p>
 * <ol>
 *     <li>scan selected classes for relevant {@link AnnotatedElement elements}</li>
 *     <li>turn those elements into bean definitions and register them in the context</li>
 * </ol>
 *
 * <p>
 * This abstraction is typically used after feature selection has already resolved
 * which classes should be imported.
 * </p>
 */
public interface FeatureSelectionImporter {

    /**
     * Shared logger for feature import operations.
     */
    Logger LOGGER = LoggerFactory.getLogger(FeatureSelectionImporter.class);

    /**
     * Imports the given classes into the target {@link BeanContext}.
     *
     * <p>
     * Implementations may scan classes, extract bean-producing elements,
     * create definitions, and register them into the context.
     * </p>
     *
     * @param context target bean context
     * @param classes feature classes to import
     */
    void importClasses(BeanContext context, Class<?>... classes);

    /**
     * Default importer implementation based on a {@link BeanScanner}. 📦
     *
     * <p>
     * This importer scans provided classes for {@link AnnotatedElement elements},
     * sorts them using {@link Sorter#REFLECTION_TYPE_COMPARATOR}, creates bean
     * definitions through the context's {@link BeanDefinitionFactory}, and registers
     * them one by one.
     * </p>
     *
     * <p>
     * If an element is annotated with {@link SuppressException}, matching exceptions
     * thrown during definition creation or registration are ignored.
     * </p>
     */
    class Default implements FeatureSelectionImporter {

        /**
         * Scanner used to extract bean-relevant elements from imported classes.
         */
        private final BeanScanner<AnnotatedElement> scanner;

        /**
         * Creates a default feature importer with the given scanner.
         *
         * @param scanner scanner used to discover bean-producing elements
         */
        public Default(BeanScanner<AnnotatedElement> scanner) {
            this.scanner = scanner;
        }

        /**
         * Scans the given classes and registers discovered bean definitions.
         *
         * <p>
         * Processing flow:
         * </p>
         * <ol>
         *     <li>obtain {@link BeanDefinitionFactory} from the context</li>
         *     <li>scan provided classes for {@link AnnotatedElement elements}</li>
         *     <li>sort elements for deterministic registration order</li>
         *     <li>create and register bean definitions</li>
         *     <li>optionally suppress matching exceptions declared with {@link SuppressException}</li>
         * </ol>
         *
         * <p>
         * When suppression is configured on an element, the thrown exception is ignored
         * only if its type is assignable to one of the declared suppressed types.
         * Otherwise the exception is rethrown.
         * </p>
         *
         * @param context target bean context
         * @param classes feature classes to import
         */
        @Override
        public void importClasses(BeanContext context, Class<?>... classes) {
            BeanDefinitionFactory        definitionFactory = context.getBeanDefinitionFactory();
            Collection<AnnotatedElement> scanned           = scanner.scan(classes);
            List<AnnotatedElement>       elements          = new ArrayList<>(scanned);

            elements.sort(Sorter.REFLECTION_TYPE_COMPARATOR);

            LOGGER.info("Number of elements: {}", elements.size());

            SCAN:
            for (AnnotatedElement element : elements) {
                try {
                    context.registerDefinition(definitionFactory.createDefinition(element, context));
                } catch (Exception exception) {
                    Class<? extends Throwable>[] throwableTypes = getAnnotationValue(
                            element, SuppressException.class, SuppressException::value
                    );

                    if (throwableTypes != null) {
                        for (Class<? extends Throwable> throwableType : throwableTypes) {
                            if (throwableType.isAssignableFrom(exception.getClass())) {
                                continue SCAN;
                            }
                        }
                    }

                    throw exception;
                }
            }
        }
    }
}