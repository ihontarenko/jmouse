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
 * 📦 Scans for classes annotated with {@link BeanImport} and recursively imports beans.
 * ✅ Use this to modularize bean configuration.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
@Priority(-100)
public class BeanImportAnnotatedClassBeanScanner implements BeanScanner<AnnotatedElement> {

    private static final BeanScanner<AnnotatedElement> BEAN_SCANNER = new BeanFactoriesAnnotatedClassBeanScanner();

    @Override
    public Collection<AnnotatedElement> scan(Class<?>... baseClasses) {
        List<AnnotatedElement> elements = new ArrayList<>();

        for (Class<?> klass : ClassFinder.findAnnotatedClasses(BeanImport.class, baseClasses)) {
            Class<?>[] imports = Reflections.getAnnotationValue(klass, BeanImport.class, BeanImport::value);

            if (imports != null && imports.length > 0) {
                elements.addAll(BEAN_SCANNER.scan(imports));
            }
        }

        return elements;
    }
}
