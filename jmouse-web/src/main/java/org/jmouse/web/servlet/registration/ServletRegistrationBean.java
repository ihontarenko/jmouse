package org.jmouse.web.servlet.registration;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.web.http.request.multipart.MultipartProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.Introspector;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * {@link RegistrationBean} implementation for programmatic registration of a {@link Servlet}.
 * <p>
 * Wraps a servlet instance and handles its registration with the {@link ServletContext},
 * including setting URL mappings and load-on-startup priority.
 * </p>
 *
 * @param <S> the type of {@link Servlet} to register
 */
public class ServletRegistrationBean<S extends Servlet>
        extends AbstractDynamicRegistrationBean<ServletRegistration.Dynamic> {

    /**
     * Default URL pattern if no mappings are provided.
     */
    public static final  String[] DEFAULT_MAPPINGS            = {"/*"};
    public static final  String   SERVLET_MAPPINGS_ATTRIBUTE  = ServletRegistrationBean.class.getName() + ".SERVLET_MAPPINGS";
    private static final Logger   SERVLET_REGISTRATION_LOGGER = LoggerFactory.getLogger(ServletRegistrationBean.class);

    private final S                   servlet;
    private       int                 loadOnStartup = -1;
    private       Set<String>         mappings      = new LinkedHashSet<>();
    private       MultipartProperties multipartProperties;

    /**
     * Create a new {@code ServletRegistrationBean} with the given registration name and servlet instance.
     *
     * @param name    the name under which to register the servlet (may be {@code null} to use bean name)
     * @param servlet the servlet instance to register; must not be {@code null}
     */
    public ServletRegistrationBean(String name, S servlet) {
        super(name);
        this.servlet = servlet;
    }

    /**
     * Configure the {@link ServletRegistration.Dynamic} by applying URL mappings and load-on-startup.
     *
     * @param dynamic the dynamic registration handle obtained from the container; never {@code null}
     */
    @Override
    public void configure(ServletRegistration.Dynamic dynamic) {
        String[] patterns = DEFAULT_MAPPINGS;
        MultipartProperties properties = getMultipartProperties();

        if (!getMappings().isEmpty()) {
            patterns = getMappings().toArray(String[]::new);
        }

        if (properties != null && properties.isEnabled()) {
            dynamic.setMultipartConfig(properties.getMultipartConfig());
        }

        dynamic.addMapping(patterns);
        dynamic.setLoadOnStartup(getLoadOnStartup());
    }

    /**
     * Perform the actual registration of the servlet with the {@link ServletContext}.
     *
     * @param name the registration name (never {@code null})
     * @param sc   the target {@link ServletContext} (never {@code null})
     * @return the {@link ServletRegistration.Dynamic} handle, or {@code null} if registration failed
     */
    @Override
    protected ServletRegistration.Dynamic doRegistration(String name, ServletContext sc) {
        ServletRegistration.Dynamic registration = sc.addServlet(getServletName(), getServlet());

        if (registration != null) {
            SERVLET_REGISTRATION_LOGGER.info("Servlet '{}' registered under name '{}' with mappings {}",
                                             getServlet().getClass().getName(),
                                             getServletName(),
                                             getMappings());
            sc.setAttribute(SERVLET_MAPPINGS_ATTRIBUTE, getMappings());
        }

        return registration;
    }

    /**
     * Return the servlet instance to be registered.
     *
     * @return the underlying servlet
     */
    public S getServlet() {
        return servlet;
    }

    /**
     * Determine the name under which to register the servlet. If an explicit name
     * was provided, it is returned; otherwise the short class name (decapitalized)
     * of the servlet instance is used.
     *
     * @return the servlet registration name (never {@code null})
     */
    public String getServletName() {
        String servletName = getName();

        if (servletName == null) {
            servletName = Introspector.decapitalize(Reflections.getShortName(getServlet()));
        }

        return servletName;
    }

    /**
     * Return the load-on-startup priority.
     *
     * @return the load-on-startup order (lower means earlier), or -1 if unspecified
     */
    public int getLoadOnStartup() {
        return loadOnStartup;
    }

    /**
     * Set the load-on-startup priority for this servlet.
     *
     * @param loadOnStartup the startup order (lower values are loaded earlier)
     */
    public void setLoadOnStartup(int loadOnStartup) {
        this.loadOnStartup = loadOnStartup;
    }

    /**
     * Return the configured URL pattern mappings.
     *
     * @return a set of URL patterns (never {@code null})
     */
    public Set<String> getMappings() {
        return mappings;
    }

    /**
     * Replace the current URL pattern mappings with the given set.
     *
     * @param mappings the new URL patterns; must not be {@code null}
     */
    public void setMappings(Set<String> mappings) {
        this.mappings = new HashSet<>(mappings);
    }

    /**
     * Add one or more URL patterns to the existing mapping set.
     *
     * @param mappings one or more URL patterns to add; must not be {@code null}
     */
    public void addMappings(String... mappings) {
        this.mappings.addAll(Set.of(mappings));
    }

    /**
     * 📦 Get multipart request handling properties.
     *
     * @return the configured multipart properties
     */
    public MultipartProperties getMultipartProperties() {
        return multipartProperties;
    }

    /**
     * ⚙️ Set multipart request handling properties.
     *
     * @param multipartProperties the multipart properties to use
     */
    public void setMultipartProperties(MultipartProperties multipartProperties) {
        this.multipartProperties = multipartProperties;
    }

    /**
     * Return a short description for this registration (for logging/debugging).
     *
     * @return a description string
     */
    @Override
    public String getDescription() {
        return "servlet " + getName();
    }

    /**
     * Determine the order of this registration relative to others.
     *
     * @return the order value (lower =&gt; higher priority)
     */
    @Override
    public int getOrder() {
        return -1000 + getLoadOnStartup();
    }

    @Override
    public String toString() {
        return "ServletRegistrationBean[%d][name='%s', servlet=%s, mappings=%s, loadOnStartup=%d]".formatted(
                getOrder(), getServletName(), servlet.getClass().getSimpleName(), mappings, loadOnStartup);
    }
}
