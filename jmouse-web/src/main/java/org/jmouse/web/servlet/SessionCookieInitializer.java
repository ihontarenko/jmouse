package org.jmouse.web.servlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.SessionCookieConfig;
import org.jmouse.beans.annotation.Ignore;
import org.jmouse.web.configuration.SessionProperties;
import org.jmouse.web.initializer.WebApplicationInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

@Ignore
public class SessionCookieInitializer implements WebApplicationInitializer {

    private static final Logger                      LOGGER = LoggerFactory.getLogger(SessionCookieInitializer.class);
    private final        Supplier<SessionProperties> supplier;

    public SessionCookieInitializer(Supplier<SessionProperties> supplier) {
        this.supplier = supplier;
    }

    @Override
    public void onStartup(ServletContext servletContext) {
        SessionProperties properties = supplier.get();

        if (properties != null) {
            SessionProperties.Cookie cookie = properties.getCookie();
            SessionCookieConfig      config = servletContext.getSessionCookieConfig();

            config.setName(cookie.name());
            config.setPath(cookie.path());
            config.setDomain(cookie.domain());
            config.setMaxAge(cookie.maxAge());
            config.setHttpOnly(cookie.httpOnly());
            config.setSecure(cookie.secure());

            LOGGER.info("Session configured: {}", cookie);
        }
    }

}
