package org.jmouse.context;

import org.jmouse.beans.BeansScannerBeanContextInitializer;
import org.jmouse.core.Priority;

/**
 * 🔍 Extends the base BeansScannerBeanContextInitializer to add
 * scanning support for classes annotated with {@link BeanProperties}.
 * <p>
 * Used to enhance bean discovery by including bean properties scanning.
 * </p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
@Priority(-1000)
public class ApplicationContextBeansScanner extends BeansScannerBeanContextInitializer {

    public ApplicationContextBeansScanner(Class<?>... baseClasses) {
        super(baseClasses);

        addScanner(new BeanPropertiesClassBeanScanner());
    }
}
