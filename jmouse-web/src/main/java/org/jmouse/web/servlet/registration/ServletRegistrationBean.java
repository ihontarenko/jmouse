package org.jmouse.web.servlet.registration;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import org.jmouse.beans.annotation.SuppressException;
import org.jmouse.beans.definition.BeanDefinitionException;
import org.jmouse.core.reflection.Reflections;

import java.beans.Introspector;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

@SuppressException({BeanDefinitionException.class})
public class ServletRegistrationBean<S extends Servlet>
        extends AbstractDynamicRegistrationBean<ServletRegistration.Dynamic> {

    public static final String[] DEFAULT_MAPPINGS = {"/*"};

    private final S           servlet;
    private       int         loadOnStartup = -1;
    private       Set<String> mappings      = new LinkedHashSet<>();

    public ServletRegistrationBean(String name, S servlet) {
        super(name);
        this.servlet = servlet;
    }

    @Override
    public void configure(ServletRegistration.Dynamic dynamic) {
        String[] mappings = DEFAULT_MAPPINGS;

        if (!getMappings().isEmpty()) {
            mappings = getMappings().toArray(String[]::new);
        }

        dynamic.addMapping(mappings);
        dynamic.setLoadOnStartup(getLoadOnStartup());
    }

    @Override
    protected ServletRegistration.Dynamic doRegistration(String name, ServletContext servletContext) {
        return servletContext.addServlet(getServletName(), getServlet());
    }

    public S getServlet() {
        return servlet;
    }

    public String getServletName() {
        String servletName = getName();

        if (servletName == null) {
            servletName = Introspector.decapitalize(Reflections.getShortName(getServlet()));
        }

        return servletName;
    }

    public int getLoadOnStartup() {
        return loadOnStartup;
    }

    public void setLoadOnStartup(int loadOnStartup) {
        this.loadOnStartup = loadOnStartup;
    }

    public Set<String> getMappings() {
        return mappings;
    }

    public void setMappings(Set<String> mappings) {
        this.mappings = new HashSet<>(mappings);
    }

    public void addMappings(String... mappings) {
        this.mappings.addAll(Set.of(mappings));
    }
}
