package org.jmouse.util;

import java.net.URI;

public class UrlHelper {

    static URI toURI(String value) {
        return URI.create(value);
    }

}
