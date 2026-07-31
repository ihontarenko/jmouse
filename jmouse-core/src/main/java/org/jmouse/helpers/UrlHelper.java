package org.jmouse.helpers;

import java.net.URI;

public class UrlHelper {

    static URI toURI(String value) {
        return URI.create(value);
    }

}
