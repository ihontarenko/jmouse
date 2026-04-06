package org.jmouse.beans.scanner;

import org.jmouse.beans.BeanScanner;
import org.jmouse.beans.annotation.BeanImport;
import org.jmouse.core.reflection.ClassFinder;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.core.Priority;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 📦 {@link BeanScanner} that processes {@link BeanImport}-annotated classes and
 * recursively scans imported configurations.
 *
 * <p>
 * This scanner acts as a composition mechanism for bean definitions:
 * it discovers classes annotated with {@link BeanImport}, extracts their
 * declared imports, and delegates scanning of those imports to an underlying
 * {@link BeanFactoriesAnnotatedClassBeanScanner}.
 * </p>
 *
 * <p>
 * Effectively, this enables modular configuration where one class can
 * aggregate multiple bean sources via {@link BeanImport}.
 * </p>
 *
 * <h3>Processing flow</h3>
 * <ol>
 *     <li>Locate all classes annotated with {@link BeanImport}</li>
 *     <li>Extract imported classes via {@link BeanImport#value()}</li>
 *     <li>Delegate scanning of imported classes to a nested scanner</li>
 *     <li>Aggregate all discovered {@link AnnotatedElement}s</li>
 * </ol>
 *
 * <p>
 * ⚠️ Recursive imports are supported but must be well-structured to avoid cycles.
 * </p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
@Priority(-100)
public class BeanImportAnnotatedClassBeanScanner implements BeanScanner<AnnotatedElement> {

    /**
     * 🔗 Delegate scanner used to process imported classes.
     *
     * <p>
     * Typically scans for bean factory methods or other bean-producing elements.
     * </p>
     */
    private static final BeanScanner<AnnotatedElement> BEAN_SCANNER =
            new BeanFactoriesAnnotatedClassBeanScanner();

    /**
     * 🔍 Scans the provided base classes for {@link BeanImport} annotations
     * and recursively processes declared imports.
     *
     * @param baseClasses root classes to start scanning from
     * @return collection of discovered bean-producing {@link AnnotatedElement}s
     */
    @Override
    public Collection<AnnotatedElement> scan(Class<?>... baseClasses) {
        List<AnnotatedElement> elements = new ArrayList<>();

        for (Class<?> klass : ClassFinder.findAnnotatedClasses(BeanImport.class, baseClasses)) {
            elements.addAll(getElements(klass));
        }

        return elements;
    }

    /**
     * 📦 Resolves bean elements declared through a {@link BeanImport} source.
     *
     * <p>
     * Reads imported classes from the given {@link BeanImport}-annotated element
     * and delegates their processing to the nested {@link #BEAN_SCANNER}.
     * </p>
     *
     * <p>
     * This method does not scan the given element itself as a bean factory source.
     * Instead, it treats it as an import holder that contributes bean-producing
     * elements indirectly through its declared imports.
     * </p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * @BeanImport(WebConfig.class)
     * class RootConfig {}
     *
     * // Result:
     * // - bean factory elements discovered from WebConfig
     * }</pre>
     *
     * @param element source element annotated with {@link BeanImport}
     * @return imported bean-producing elements, or an empty collection if no imports are declared
     */
    public Collection<AnnotatedElement> getElements(AnnotatedElement element) {
        List<AnnotatedElement> elements = new ArrayList<>();

        Class<?>[] imports = Reflections.getAnnotationValue(
                element,
                BeanImport.class,
                BeanImport::value
        );

        if (imports != null && imports.length > 0) {
            elements.addAll(BEAN_SCANNER.scan(imports));
        }

        return elements;
    }

}