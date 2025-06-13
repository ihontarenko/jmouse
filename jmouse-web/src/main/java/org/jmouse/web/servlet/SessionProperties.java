package org.jmouse.web.servlet;

import jakarta.servlet.SessionTrackingMode;
import org.jmouse.context.BeanProperties;
import org.jmouse.core.bind.BindDefault;

import java.util.Set;

@BeanProperties("jmouse.web.server.session")
public class SessionProperties {

    private Cookie                   cookie;
    private int                      timeout;
    private Set<SessionTrackingMode> trackingMode;

    public int getTimeout() {
        return timeout;
    }

    @BindDefault("60")
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public Cookie getCookie() {
        return cookie;
    }

    public void setCookie(Cookie cookie) {
        this.cookie = cookie;
    }

    public Set<SessionTrackingMode> getTrackingMode() {
        return trackingMode;
    }

    public void setTrackingMode(Set<SessionTrackingMode> trackingMode) {
        this.trackingMode = trackingMode;
    }

    public record Cookie(
            @BindDefault("JMOUSE_SESSION_ID") String name,
            @BindDefault("/") String path,
            @BindDefault("900000") int maxAge,
            @BindDefault("true") boolean httpOnly,
            @BindDefault("true") boolean secure
    ) { }

}
