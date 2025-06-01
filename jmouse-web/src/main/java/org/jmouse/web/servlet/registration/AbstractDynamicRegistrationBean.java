package org.jmouse.web.servlet.registration;

import jakarta.servlet.Registration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base class for dynamic registration beans (servlets, filters, etc.) that produce a
 * {@link Registration.Dynamic} handle. Handles common configuration steps such as
 * async support and init parameters.
 *
 * @param <D> the specific type of {@link Registration.Dynamic} returned by {@link #doRegistration}
 */
abstract public class AbstractDynamicRegistrationBean<D extends Registration.Dynamic> extends AbstractRegistrationBean {

    private boolean             asyncSupported = true;
    private Map<String, String> initParameters = new LinkedHashMap<>();

    /**
     * Create a new dynamic registration bean with the given name.
     *
     * @param name the registration name (may be {@code null}); if {@code null}, bean name will be used
     */
    protected AbstractDynamicRegistrationBean(String name) {
        super(name);
    }

    /**
     * Perform the actual registration of the component with the {@link ServletContext}.
     * Called by {@link #register(ServletContext)} to obtain the {@link Registration.Dynamic}
     * instance for further configuration.
     *
     * @param name           the name under which to register the component (never {@code null})
     * @param servletContext the target {@link ServletContext} (never {@code null})
     * @return the {@link Registration.Dynamic} handle, or {@code null} if registration failed
     * @throws ServletException if an error occurs during registration
     */
    abstract protected D doRegistration(String name, ServletContext servletContext) throws ServletException;

    /**
     * Register the underlying component with the given {@link ServletContext}. First invokes
     * {@link #doRegistration} to obtain a {@link Registration.Dynamic}. If that returns {@code null},
     * throws {@link IllegalStateException}. Otherwise calls {@link #configure} on the dynamic handle.
     *
     * @param servletContext the target {@link ServletContext}; must not be {@code null}
     * @throws ServletException         on registration failure
     * @throws IllegalArgumentException if {@code servletContext} is {@code null}
     * @throws IllegalStateException    if {@link #doRegistration} returns {@code null}
     */
    @Override
    public void register(ServletContext servletContext) throws ServletException {
        if (servletContext == null) {
            throw new IllegalArgumentException("ServletContext must not be null");
        }

        D dynamic = doRegistration(getName(), servletContext);

        if (dynamic == null) {
            throw new IllegalStateException("Unable to register '" + getName() + "' to servlet context");
        }

        configure(dynamic);
    }

    /**
     * Configure the given {@link Registration.Dynamic} with async support and any init parameters.
     *
     * @param dynamic the {@link Registration.Dynamic} to configure; must not be {@code null}
     * @throws IllegalArgumentException if {@code dynamic} is {@code null}
     */
    public void configure(D dynamic) {
        if (dynamic == null) {
            throw new IllegalArgumentException("Registration.Dynamic must not be null");
        }

        dynamic.setAsyncSupported(isAsyncSupported());

        if (!initParameters.isEmpty()) {
            initParameters.forEach(dynamic::setInitParameter);
        }
    }

    /**
     * Return whether async mode is supported for the underlying component.
     *
     * @return {@code true} if async is supported; {@code false} otherwise
     */
    public boolean isAsyncSupported() {
        return asyncSupported;
    }

    /**
     * Set whether async mode is supported.
     *
     * @param asyncSupported {@code true} to enable async support; {@code false} to disable
     */
    public void setAsyncSupported(boolean asyncSupported) {
        this.asyncSupported = asyncSupported;
    }

    /**
     * Return the init parameters to apply during configuration.
     *
     * @return an unmodifiable {@link Map} of init parameters (never {@code null})
     */
    public Map<String, String> getInitParameters() {
        return initParameters;
    }

    /**
     * Replace all init parameters with the given map.
     *
     * @param initParameters the map of parameters to apply; must not be {@code null}
     * @throws IllegalArgumentException if {@code initParameters} is {@code null}
     */
    public void setInitParameters(Map<String, String> initParameters) {
        if (initParameters == null) {
            throw new IllegalArgumentException("Init parameters map must not be null");
        }

        this.initParameters = new LinkedHashMap<>(initParameters);
    }

    /**
     * Add a single init parameter to be applied during configuration.
     *
     * @param name  the init parameter name; must not be {@code null} or empty
     * @param value the init parameter value; may be {@code null}
     * @throws IllegalArgumentException if {@code name} is {@code null} or blank
     */
    public void setInitParameter(String name, String value) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Init parameter name must not be null or blank");
        }

        this.initParameters.put(name, value);
    }
}
