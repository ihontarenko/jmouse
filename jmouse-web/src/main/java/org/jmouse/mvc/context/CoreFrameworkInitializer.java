package org.jmouse.mvc.context;

import org.jmouse.beans.ScannerBeanContextInitializer;
import org.jmouse.util.Priority;

@Priority(100)
public class CoreFrameworkInitializer extends ScannerBeanContextInitializer {

    public CoreFrameworkInitializer(Class<?>... basePackages) {
        super(basePackages);
    }

}
