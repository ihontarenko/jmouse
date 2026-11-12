package org.jmouse.web.servlet;

import jakarta.servlet.SessionTrackingMode;
import java.util.EnumSet;
import java.util.Set;
import org.jmouse.context.BeanProperties;
import org.jmouse.core.binding.BindDefault;

/**
 * 📦 Session configuration properties used to control cookie, timeout, and tracking behavior.
 *
 * <p>Values are typically bound from external configuration sources (e.g., properties or YAML files)
 * using the prefix {@code jmouse.web.server.session}.</p>
 *
 * <h3>🔧 Example properties file</h3>
 * <pre>{@code
 * jmouse.web.server.session.timeout=120
 * jmouse.web.server.session.tracking-mode=COOKIE
 * jmouse.web.server.session.cookie.name=MYSESSION
 * jmouse.web.server.session.cookie.secure=true
 * }</pre>
 *
 * <h3>⚙️ Default values</h3>
 * <ul>
 *     <li>{@code timeout} = 60 (minutes)</li>
 *     <li>{@code cookie.name} = JMOUSE_SESSION_ID</li>
 *     <li>{@code cookie.path} = /</li>
 *     <li>{@code cookie.maxAge} = 900000 (seconds)</li>
 *     <li>{@code cookie.httpOnly} = true</li>
 *     <li>{@code cookie.secure} = true</li>
 * </ul>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 * @see SessionConfigurationInitializer
 * @see jakarta.servlet.SessionCookieConfig
 * @see jakarta.servlet.SessionTrackingMode
 */
@BeanProperties("jmouse.web.server.session")
public class SessionProperties {

    private Cookie cookie = new Cookie("JMOUSE_SESSION_ID", "/", -1, true, true);
    private int timeout = 60;
    private Set<SessionTrackingMode> trackingMode = EnumSet.of(SessionTrackingMode.COOKIE);

    /**
     * ⏱️ Session timeout in minutes.
     * Defaults to 60 if not explicitly configured.
     *
     * @return timeout value in minutes
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Sets the session timeout.
     *
     * @param timeout timeout duration in minutes
     */
    @BindDefault("60")
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * 🍪 Session cookie configuration.
     *
     * @return cookie config object
     */
    public Cookie getCookie() {
        return cookie;
    }

    /**
     * Sets the session cookie configuration.
     *
     * @param cookie cookie settings
     */
    public void setCookie(Cookie cookie) {
        this.cookie = cookie;
    }

    /**
     * 🌐 Set of enabled session tracking modes (e.g., COOKIE, URL).
     *
     * @return the tracking modes
     */
    public Set<SessionTrackingMode> getTrackingMode() {
        return trackingMode;
    }

    /**
     * Sets the session tracking modes.
     *
     * @param trackingMode session tracking strategies
     */
    public void setTrackingMode(Set<SessionTrackingMode> trackingMode) {
        this.trackingMode = trackingMode;
    }

    /**
     * 🍪 Configuration for the session cookie.
     *
     * @param name     cookie name (default: {@code JMOUSE_SESSION_ID})
     * @param path     cookie path (default: {@code /})
     * @param maxAge   max age of the cookie in seconds (default: {@code 900000})
     * @param httpOnly whether the cookie is HTTP-only (default: {@code true})
     * @param secure   whether the cookie is secure (sent only over HTTPS) (default: {@code true})
     */
    public record Cookie(
        @BindDefault("JMOUSE_SESSION_ID") String name,
        @BindDefault("/") String path,
        @BindDefault("900000") int maxAge,
        @BindDefault("true") boolean httpOnly,
        @BindDefault("true") boolean secure
    ) {}
}
