package org.jmouse.web.initializer.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jmouse.testing_ground.beans.BeanContext;
import org.jmouse.testing_ground.beans.BeanContextInitializer;
import org.jmouse.util.Priority;

@Priority(1)
public class ApplicationBeanContextInitializer implements BeanContextInitializer {

    private final static Logger LOGGER = LoggerFactory.getLogger(ApplicationBeanContextInitializer.class);

    /**
     * Initializes the given {@link BeanContext}.
     *
     * @param context the {@link BeanContext} to initialize.
     */
    @Override
    public void initialize(BeanContext context) {

    }

}
