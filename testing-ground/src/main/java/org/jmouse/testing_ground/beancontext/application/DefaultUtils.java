package org.jmouse.testing_ground.beancontext.application;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanContextAware;
import org.jmouse.beans.annotation.Provide;

@Provide(value = "util", proxied = true)
public class DefaultUtils implements Utils, BeanContextAware {

    private BeanContext context;

    @Override
    public String getOsName() {
        return System.getProperty("os.name");
    }

    /**
     * Sets the {@link BeanContext} for this component.
     *
     * @param context the {@link BeanContext} to set.
     */
    @Override
    public void setBeanContext(BeanContext context) {
        this.context = context;
    }

    /**
     * Retrieves the {@link BeanContext} associated with this component.
     *
     * @return the current {@link BeanContext}.
     */
    @Override
    public BeanContext getBeanContext() {
        return context;
    }
}
