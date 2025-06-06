package org.jmouse.web.configuration;

import org.jmouse.context.BeanProperties;
import org.jmouse.core.bind.BindDefault;

@BeanProperties("jmouse.web.server.session")
public class SessionProperties {

    private Cookie cookie;

    public Cookie getCookie() {
        return cookie;
    }

    public void setCookie(Cookie cookie) {
        this.cookie = cookie;
    }

    public record Cookie(
            @BindDefault("JMOUSE_SESSION_ID") String name,
            @BindDefault("/") String path,
            @BindDefault("900000") int maxAge,
            @BindDefault("true") boolean httpOnly,
            @BindDefault("true") boolean secure
    ) {}

}
