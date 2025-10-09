package org.jmouse.web.http.writers;

import org.jmouse.web.http.WWWAuthenticate;

public class WWWAuthenticateWriter extends AbstractHeaderWriter {

    public WWWAuthenticateWriter(WWWAuthenticate wwwAuthenticate) {
        super(wwwAuthenticate);
    }

}
