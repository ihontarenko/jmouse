package org.jmouse.web.servlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.SessionCookieConfig;
import org.jmouse.beans.annotation.Ignore;
import org.jmouse.core.AttributeMapper;
import org.jmouse.core.Priority;
import org.jmouse.web.initializer.WebApplicationInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * 🛠️ Configures the servlet session cookie and tracking settings
 * at application startup using values from {@link SessionProperties}.
 *
 * <p>🔐 This class is typically registered as a {@link WebApplicationInitializer}
 * to perform early configuration of session behavior, including:</p>
 *
 * <ul>
 *     <li>🍪 Session cookie name, path, max age, security flags</li>
 *     <li>🕒 Session timeout duration</li>
 *     <li>📦 Session tracking mode (e.g., COOKIE, URL)</li>
 * </ul>
 * <p>
 * 🧩 Uses {@link AttributeMapper} to apply optional values in a concise and
 * null-safe manner.
 *
 * <h3>🔁 Example</h3>
 * <pre>{@code
 * new SessionConfigurationInitializer(() -> new SessionProperties(...)).onStartup(ctx);
 * }</pre>
 *
 * <p>Logs the applied configuration once it's set.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 * @see SessionProperties
 * @see SessionCookieConfig
 * @see WebApplicationInitializer
 * @see AttributeMapper
 */
@Ignore
@Priority(-500)
public class SessionConfigurationInitializer implements WebApplicationInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionConfigurationInitializer.class);

    private final Supplier<SessionProperties> supplier;

    /**
     * 🏗️ Constructs a new initializer that supplies {@link SessionProperties}
     * from the provided {@link Supplier}.
     *
     * @param supplier supplier of session properties; must not be {@code null}
     */
    public SessionConfigurationInitializer(Supplier<SessionProperties> supplier) {
        this.supplier = supplier;
    }

    /**
     * 🧯 Called by the servlet container on application startup.
     * Configures the session cookie, tracking mode, and timeout.
     *
     * @param servletContext the {@link ServletContext} of the current web application
     */
    @Override
    public void onStartup(ServletContext servletContext) {
        SessionProperties properties = supplier.get();

        if (properties != null) {
            SessionProperties.Cookie cookie = properties.getCookie();
            SessionCookieConfig      config = servletContext.getSessionCookieConfig();
            AttributeMapper          mapper = AttributeMapper.get();

            // 🍪 Apply cookie settings (if non-null)
            mapper.get(cookie::name).accept(config::setName);
            mapper.get(cookie::path).accept(config::setPath);
            mapper.get(cookie::maxAge).accept(config::setMaxAge);
            mapper.get(cookie::httpOnly).accept(config::setHttpOnly);
            mapper.get(cookie::secure).accept(config::setSecure);

            // 🔄 Tracking mode
            if (properties.getTrackingMode() != null) {
                servletContext.setSessionTrackingModes(properties.getTrackingMode());
            }

            // 🕒 Timeout
            servletContext.setSessionTimeout(properties.getTimeout());

            LOGGER.info("Session configured: {}; Tracking modes: {}",
                    cookie, servletContext.getEffectiveSessionTrackingModes());
        }
    }
}
