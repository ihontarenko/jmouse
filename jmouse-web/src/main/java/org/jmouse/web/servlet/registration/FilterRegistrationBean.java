package org.jmouse.web.servlet.registration;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import org.jmouse.beans.annotation.Ignore;
import org.jmouse.util.Priority;

import java.util.*;

/**
 * {@link RegistrationBean} implementation for programmatic registration of a {@link Filter}.
 * <p>
 * During servlet container startup, this bean registers the filter under a given name,
 * applies initialization parameters, configures async support, and maps it to specified URL patterns
 * and dispatcher types.
 * </p>
 *
 * @param <F> the type of {@link Filter} to register
 */
@Ignore
@Priority(-2000)
public class FilterRegistrationBean<F extends Filter>
        extends AbstractDynamicRegistrationBean<FilterRegistration.Dynamic> {

    private static final String[] DEFAULT_URL_MAPPINGS = {"/*"};

    private F                       filter;
    private EnumSet<DispatcherType> dispatcherTypes;
    private Set<String>             mappings   = new LinkedHashSet<>();
    private boolean                 matchAfter = false;

    /**
     * Create a new {@code FilterRegistrationBean} without an explicit name.
     * The bean name or class-derived name will be used at registration.
     *
     * @param filter the filter instance to register; must not be {@code null}
     */
    public FilterRegistrationBean(F filter) {
        this(null, filter);
    }

    /**
     * Create a new {@code FilterRegistrationBean} with the given registration name.
     *
     * @param name   the name under which to register the filter (may be {@code null})
     * @param filter the filter instance to register; must not be {@code null}
     */
    public FilterRegistrationBean(String name, F filter) {
        super(name);
        this.filter = filter;
    }

    /**
     * Perform the actual servlet container registration of the filter.
     * <p>
     * Invoked by {@link #register(ServletContext)} to obtain the dynamic registration handle.
     * </p>
     *
     * @param name           the name under which to register the filter (never {@code null})
     * @param servletContext the target {@link ServletContext} (never {@code null})
     * @return the {@link FilterRegistration.Dynamic} handle, or {@code null} if registration failed
     */
    @Override
    protected FilterRegistration.Dynamic doRegistration(String name, ServletContext servletContext) {
        return servletContext.addFilter(getName(), getFilter());
    }

    /**
     * Configure the given {@link FilterRegistration.Dynamic} with async support, init parameters,
     * and URL pattern/dispatcher type mappings.
     *
     * @param dynamic the {@link FilterRegistration.Dynamic} to configure; never {@code null}
     */
    @Override
    public void configure(FilterRegistration.Dynamic dynamic) {
        super.configure(dynamic);

        String[] patterns = getMappings().isEmpty() ? DEFAULT_URL_MAPPINGS : getMappings().toArray(String[]::new);

        dynamic.addMappingForUrlPatterns(getDispatcherTypes(), isMatchAfter(), patterns);
    }

    /**
     * Return the underlying filter instance.
     *
     * @return the filter being registered
     */
    public F getFilter() {
        return filter;
    }

    /**
     * Set the filter instance to register.
     *
     * @param filter the filter to register; must not be {@code null}
     */
    public void setFilter(F filter) {
        this.filter = filter;
    }

    /**
     * Return the configured URL patterns.
     *
     * @return a set of URL patterns (never {@code null})
     */
    public Collection<String> getMappings() {
        return this.mappings;
    }

    /**
     * Replace the configured URL patterns for filter mapping.
     * If not set, the default pattern {@code "/*"} will be used.
     *
     * @param mappings the URL patterns to map the filter to; must not be {@code null}
     */
    public void setMappings(Collection<String> mappings) {
        this.mappings = new HashSet<>(mappings);
    }

    /**
     * Add one or more URL patterns to the existing mapping set.
     *
     * @param urlPatterns the URL patterns to add; must not be {@code null}
     */
    public void addUrlPatterns(String... urlPatterns) {
        Collections.addAll(this.mappings, urlPatterns);
    }

    /**
     * Set the dispatcher types under which the filter will be invoked.
     * <p>
     * For example: {@code EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD)}.
     * If not set, defaults to {@link DispatcherType#REQUEST}.
     * </p>
     *
     * @param first the first dispatcher type (must not be {@code null})
     * @param rest  optional additional dispatcher types
     */
    public void setDispatcherTypes(DispatcherType first, DispatcherType... rest) {
        this.dispatcherTypes = EnumSet.of(first, rest);
    }

    /**
     * Return the configured dispatcher types. Defaults to {@link DispatcherType#REQUEST}
     * if none have been set.
     *
     * @return the set of dispatcher types (never {@code null})
     */
    public EnumSet<DispatcherType> getDispatcherTypes() {
        if (dispatcherTypes == null) {
            return EnumSet.of(DispatcherType.REQUEST);
        }
        return this.dispatcherTypes;
    }

    /**
     * Replace the entire set of dispatcher types under which the filter will be invoked.
     *
     * @param dispatcherTypes the set of dispatcher types; must not be {@code null}
     */
    public void setDispatcherTypes(EnumSet<DispatcherType> dispatcherTypes) {
        this.dispatcherTypes = dispatcherTypes;
    }

    /**
     * Return a description of this registration (used for logging/debugging).
     *
     * @return a brief description of the filter registration
     */
    @Override
    public String getDescription() {
        return "filter " + getName();
    }

    /**
     * Determine whether the filter mapping should be added after existing mappings
     * ({@code true}) or before them ({@code false}).
     *
     * @return {@code true} if this filter should match after existing filters; {@code false} otherwise
     */
    public boolean isMatchAfter() {
        return matchAfter;
    }

    /**
     * Set whether the filter mapping should match after existing filters.
     *
     * @param matchAfter {@code true} to add mapping after existing entries; {@code false} to add before
     */
    public void setMatchAfter(boolean matchAfter) {
        this.matchAfter = matchAfter;
    }
}
