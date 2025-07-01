package org.jmouse.context;

import org.jmouse.beans.BeanScanner;
import org.jmouse.context.BeanProperties;
import org.jmouse.core.reflection.ClassFinder;
import org.jmouse.util.Priority;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collection;

/**
 * 🔍 Scans for classes annotated with {@link BeanProperties}.
 *
 * <p>
 * This scanner is used to locate configuration or metadata classes that define
 * properties bindings via {@code @BeanProperties} annotation.
 * </p>
 *
 * <pre>{@code
 * @BeanProperties("application.properties")
 * public class AppConfig { ... }
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
@Priority(1)
public class BeanPropertiesClassBeanScanner implements BeanScanner<AnnotatedElement> {

    @Override
    public Collection<AnnotatedElement> scan(Class<?>... baseClasses) {
        return new ArrayList<>(
                ClassFinder.findAnnotatedClasses(BeanProperties.class, baseClasses)
        );
    }
}
