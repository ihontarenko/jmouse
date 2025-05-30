package org.jmouse.web.servlet.registration;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.jmouse.beans.BeanNameKeeper;
import org.jmouse.web.initializer.WebApplicationInitializer;

abstract public class AbstractRegistrationBean implements RegistrationBean, WebApplicationInitializer, BeanNameKeeper {

    private boolean enabled;
    private final String name;
    private  String beanName;
    private       int    order;

    protected AbstractRegistrationBean(String name) {
        this.name = name;
    }

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        String name = getName();

        if (!isEnabled()) {
            LOGGER.info("Registration for '{}' is disabled", name);
            return;
        }

        register(servletContext);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Short name of component.
     */
    @Override
    public String getName() {
        String name = this.name;

        if (name == null) {
            name = this.beanName;
        }

        return name;
    }

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    /**
     * Order of this registration; lower values have higher priority.
     */
    @Override
    public int getOrder() {
        int order = this.order;
        int parentOrder = RegistrationBean.super.getOrder();

        if (parentOrder != 0 && order != parentOrder) {
            order = parentOrder;
        }

        return order;
    }

    /**
     * Set order of this registration; lower values have higher priority.
     *
     * @param int order
     */
    @Override
    public void setOrder(int order) {
        this.order = order;
    }

}
