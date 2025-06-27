package org.jmouse.beans;

import org.jmouse.beans.scanner.BeanAnnotatedClassesBeanScanner;
import org.jmouse.beans.scanner.BeanFactoriesAnnotatedClassBeanScanner;

public class BeansScannerBeanContextInitializer extends ScannerBeanContextInitializer {

    public BeansScannerBeanContextInitializer(Class<?>... baseClasses) {
        super(baseClasses);

        addScanner(new BeanAnnotatedClassesBeanScanner());
        addScanner(new BeanFactoriesAnnotatedClassBeanScanner());
    }
}
