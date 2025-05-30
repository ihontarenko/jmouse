package org.jmouse.web.servlet.registration;

import jakarta.servlet.Registration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import java.util.LinkedHashMap;
import java.util.Map;

abstract public class AbstractDynamicRegistrationBean<D extends Registration.Dynamic> extends AbstractRegistrationBean {

    private boolean             asyncSupported = true;
    private Map<String, String> initParameters = new LinkedHashMap<>();

    protected AbstractDynamicRegistrationBean(String name) {
        super(name);
    }

    abstract protected D doRegistration(String name, ServletContext servletContext);

    /**
     * Register the underlying component with the given ServletContext.
     *
     * @param servletContext the target ServletContext
     * @throws ServletException on registration failure
     */
    @Override
    public void register(ServletContext servletContext) throws ServletException {
        D dynamic = doRegistration(getName(), servletContext);

        if (dynamic == null) {
            throw new IllegalStateException("Unable to register '%s' to servlet context".formatted(getName()));
        }

        configure(dynamic);
    }

    public void configure(D dynamic) {
        Map<String, String> initParameters = getInitParameters();

        dynamic.setAsyncSupported(isAsyncSupported());

        if (!initParameters.isEmpty()) {
            initParameters.forEach(dynamic::setInitParameter);
        }
    }

    public boolean isAsyncSupported() {
        return asyncSupported;
    }

    public void setAsyncSupported(boolean asyncSupported) {
        this.asyncSupported = asyncSupported;
    }

    public Map<String, String> getInitParameters() {
        return initParameters;
    }

    public void setInitParameters(Map<String, String> initParameters) {
        this.initParameters = new LinkedHashMap<>(initParameters);
    }

    public void setInitParameter(String name, String initValue) {
        this.initParameters.put(name, initValue);
    }

}
