package org.jmouse.beans;

import org.jmouse.beans.scanner.BeanAnnotatedClassesBeanScanner;
import org.jmouse.beans.scanner.BeanFactoriesAnnotatedClassBeanScanner;
import org.jmouse.beans.scanner.BeanImportAnnotatedClassBeanScanner;
import org.jmouse.core.Priority;

/**
 * 🧪 Initializes bean context by scanning common bean annotations.
 *
 * <ul>
 *   <li>{@code @Bean} — via {@link BeanAnnotatedClassesBeanScanner}</li>
 *   <li>{@code @BeanFactories} — via {@link BeanFactoriesAnnotatedClassBeanScanner}</li>
 *   <li>{@code @BeanImport} — via {@link BeanImportAnnotatedClassBeanScanner}</li>
 * </ul>
 *
 * <pre>{@code
 * new BeansScannerBeanContextInitializer(RootAppConfig.class).initialize(context);
 * }</pre>
 *
 * 👌 Plug this into your configuration bootstrap.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
@Priority(-1000)
public class BeansScannerBeanContextInitializer extends ScannerBeanContextInitializer {

    public BeansScannerBeanContextInitializer(Class<?>... baseClasses) {
        super(baseClasses);

        addScanner(new BeanAnnotatedClassesBeanScanner());
        addScanner(new BeanFactoriesAnnotatedClassBeanScanner());
        addScanner(new BeanImportAnnotatedClassBeanScanner());
    }
}
