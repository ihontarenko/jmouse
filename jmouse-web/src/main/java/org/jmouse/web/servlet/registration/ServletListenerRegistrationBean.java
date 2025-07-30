package org.jmouse.web.servlet.registration;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.jmouse.beans.annotation.Ignore;
import org.jmouse.util.Priority;

import java.util.EventListener;

@Ignore
@Priority(-1500)
public class ServletListenerRegistrationBean<L extends EventListener> extends AbstractRegistrationBean {

    private L listener;

    /**
     * Create a new registration bean with the given explicit name.
     *
     * @param name short name for this component; may be {@code null}
     */
    public ServletListenerRegistrationBean(String name, L listener) {
        super(name);
        this.listener = listener;
    }

    /**
     * Register the underlying component with the given ServletContext.
     *
     * @param servletContext the target ServletContext
     * @throws ServletException on registration failure
     */
    @Override
    public void register(ServletContext servletContext) throws ServletException {
        servletContext.addListener(getListener());
    }

    public L getListener() {
        return listener;
    }

    public void setListener(L listener) {
        this.listener = listener;
    }

    @Override
    public String getDescription() {
        return "listener " + getName();
    }

    @Override
    public String toString() {
        return "ServletListenerRegistrationBean[%d]['%s', implementation=%s]".formatted(
                getOrder(), getDescription(), listener.getClass().getSimpleName());
    }

}
