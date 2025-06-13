package org.jmouse.web.servlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.SessionCookieConfig;
import org.jmouse.beans.annotation.Ignore;
import org.jmouse.util.AttributeMapper;
import org.jmouse.web.initializer.WebApplicationInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

@Ignore
public class SessionConfigurationInitializer implements WebApplicationInitializer {

    private static final Logger                      LOGGER = LoggerFactory.getLogger(
            SessionConfigurationInitializer.class);
    private final        Supplier<SessionProperties> supplier;

    public SessionConfigurationInitializer(Supplier<SessionProperties> supplier) {
        this.supplier = supplier;
    }

    @Override
    public void onStartup(ServletContext servletContext) {
        SessionProperties properties = supplier.get();

        if (properties != null) {
            SessionProperties.Cookie cookie = properties.getCookie();
            SessionCookieConfig      config = servletContext.getSessionCookieConfig();
            AttributeMapper          mapper = AttributeMapper.get();

            mapper.get(cookie::name).accept(config::setName);
            mapper.get(cookie::path).accept(config::setPath);
            mapper.get(cookie::maxAge).accept(config::setMaxAge);
            mapper.get(cookie::httpOnly).accept(config::setHttpOnly);
            mapper.get(cookie::secure).accept(config::setSecure);

            if (properties.getTrackingMode() != null) {
                servletContext.setSessionTrackingModes(properties.getTrackingMode());
            }

            servletContext.setSessionTimeout(properties.getTimeout());

            LOGGER.info("Session configured: {}; Tracking modes: {}",
                        cookie, servletContext.getEffectiveSessionTrackingModes());
        }
    }

}
