package org.jmouse.web.servlet.registration;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import org.jmouse.beans.annotation.Ignore;

import java.util.*;

@Ignore
public class FilterRegistrationBean<F extends Filter>
        extends AbstractDynamicRegistrationBean<FilterRegistration.Dynamic> {

    private static final String[] DEFAULT_URL_MAPPINGS = {"/*"};

    private       F                       filter;
    private       EnumSet<DispatcherType> dispatcherTypes;
    private  Set<String>             urlPatterns = new HashSet<>();
    private boolean matchAfter = false;

    public FilterRegistrationBean(String name, F filter) {
        super(name);
        this.filter = filter;
    }

    @Override
    protected FilterRegistration.Dynamic doRegistration(String name, ServletContext servletContext) {
        return servletContext.addFilter(getName(), getFilter());
    }

    /**
     * Configure the given {@link FilterRegistration.Dynamic} with async support and any init parameters.
     *
     * @param dynamic the {@link FilterRegistration.Dynamic} to configure; must not be {@code null}
     * @throws IllegalArgumentException if {@code dynamic} is {@code null}
     */
    @Override
    public void configure(FilterRegistration.Dynamic dynamic) {
        super.configure(dynamic);
        dynamic.addMappingForUrlPatterns(
                getDispatcherTypes(), isMatchAfter(), getUrlPatterns().toArray(String[]::new));
    }

    public F getFilter() {
        return filter;
    }

    public void setFilter(F filter) {
        this.filter = filter;
    }

    public void setUrlPatterns(Collection<String> urlPatterns) {
        this.urlPatterns = new HashSet<>(urlPatterns);
    }

    public Collection<String> getUrlPatterns() {
        return this.urlPatterns;
    }

    public void addUrlPatterns(String... urlPatterns) {
        Collections.addAll(this.urlPatterns, urlPatterns);
    }

    public void setDispatcherTypes(DispatcherType first, DispatcherType... rest) {
        this.dispatcherTypes = EnumSet.of(first, rest);
    }

    public void setDispatcherTypes(EnumSet<DispatcherType> dispatcherTypes) {
        this.dispatcherTypes = dispatcherTypes;
    }

    public EnumSet<DispatcherType> getDispatcherTypes() {
        if (dispatcherTypes == null) {
            return EnumSet.of(DispatcherType.REQUEST);
        }

        return this.dispatcherTypes;
    }

    @Override
    public String getDescription() {
        return "filter " + getName();
    }

    public boolean isMatchAfter() {
        return matchAfter;
    }

    public void setMatchAfter(boolean matchAfter) {
        this.matchAfter = matchAfter;
    }
}
