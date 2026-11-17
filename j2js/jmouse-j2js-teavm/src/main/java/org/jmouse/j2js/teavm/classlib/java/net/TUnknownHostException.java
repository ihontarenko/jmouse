package org.jmouse.j2js.teavm.classlib.java.net;

import java.io.IOException;

/**
 * {@code java.net.UnknownHostException} in the browser: a host that is no address — and a page resolves no name, so every name
 * that is not an address literal is one.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class TUnknownHostException extends IOException {

    public TUnknownHostException() {
    }

    public TUnknownHostException(String message) {
        super(message);
    }
}
