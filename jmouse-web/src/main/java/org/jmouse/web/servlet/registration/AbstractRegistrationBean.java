package org.jmouse.web.servlet.registration;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.jmouse.beans.BeanNameKeeper;
import org.jmouse.web.initializer.WebApplicationInitializer;

/**
 * Base class for all dynamic registration beans (servlets, filters, listeners).
 * <p>
 * Implements {@link RegistrationBean} for programmatic component registration,
 * {@link WebApplicationInitializer} to hook into servlet container startup,
 * and {@link BeanNameKeeper} to capture the bean name if not explicitly provided.
 * </p>
 *
 * @see RegistrationBean
 * @see WebApplicationInitializer
 * @see BeanNameKeeper
 */
abstract public class AbstractRegistrationBean implements RegistrationBean, WebApplicationInitializer, BeanNameKeeper {

    private final String  name;
    private       boolean enabled;
    private       String  beanName;
    private       int     order;

    /**
     * Create a new registration bean with the given explicit name.
     *
     * @param name short name for this component; may be {@code null}
     */
    protected AbstractRegistrationBean(String name) {
        this.name = name;
    }

    /**
     * Called by the servlet container during startup.  If this registration
     * is enabled, invokes {@link #register(ServletContext)}.
     *
     * @param servletContext the active {@link ServletContext}
     * @throws ServletException if registration fails
     */
    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        String name = getName();

        if (!isEnabled()) {
            LOGGER.warn("{} - disabled", getDescription());
            return;
        }

        register(servletContext);

        LOGGER.info("{} - registered", getDescription());
    }

    /**
     * Return whether this registration is enabled.  Disabled registrations
     * are skipped during {@code onStartup}.
     *
     * @return {@code true} if enabled, {@code false} otherwise
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enable or disable this registration.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Return the registration name.  If an explicit name was provided via
     * constructor, returns that; otherwise falls back to the bean name
     * as set via {@link #setBeanName(String)}.
     *
     * @return the short name of this component (never {@code null})
     */
    @Override
    public String getName() {
        String name = this.name;

        if (name == null) {
            name = this.beanName;
        }

        return name;
    }

    /**
     * Capture the Spring bean name for this registration if not set explicitly.
     *
     * @param beanName the name assigned in the bean factory
     */
    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    /**
     * Determine the order of this registration relative to others.
     * <p>
     * A lower value indicates higher priority.  If the parent
     * interface {@link RegistrationBean#order()} returns a non-zero
     * value, that value will be used unless this instance’s own order
     * has been explicitly set to match.
     * </p>
     *
     * @return the order value (lower =&gt; higher priority)
     */
    @Override
    public int order() {
        int order       = this.order;
        int parentOrder = RegistrationBean.super.order();

        if (parentOrder != 0 && order != parentOrder) {
            order = parentOrder;
        }

        return order;
    }

    /**
     * Set the order value for this registration.
     *
     * @param order lower values have higher priority
     */
    @Override
    public void setOrder(int order) {
        this.order = order;
    }

    abstract public String getDescription();

}
