package org.jmouse.core.convert.converter;

import org.jmouse.core.convert.GenericConverter;

import java.net.URI;
import java.net.URL;
import java.util.Set;

import static org.jmouse.core.convert.GenericConverter.of;

public class JavaNetConverters {

    public static Set<GenericConverter<?, ?>> getConverters() {
        return Set.of(
                // URI
                of(URI.class, URI.class, uri -> uri),
                of(URI.class, URL.class, uri -> {
                    try {
                        return uri.toURL();
                    } catch (Exception ignored) {
                        return null;
                    }
                }),
                of(URI.class, String.class, URI::toString),
                of(String.class, URI.class, URI::create),
                // URL
                of(URL.class, URL.class, url -> url),
                of(URL.class, URI.class, url -> {
                    try {
                        return url.toURI();
                    } catch (Exception ignored) {
                        return null;
                    }
                }),
                of(URL.class, String.class, URL::toString)
        );
    }

}
