package org.jmouse.core.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class CharsetResourceBundleControl extends ResourceBundle.Control {

    private static final String DEFAULT_SUFFIX = "properties";

    private final String  suffix;
    private final Charset charset;

    public CharsetResourceBundleControl(String suffix, Charset charset) {
        this.suffix = suffix;
        this.charset = charset;
    }

    public CharsetResourceBundleControl(String suffix) {
        this(suffix, StandardCharsets.UTF_8);
    }

    public CharsetResourceBundleControl(Charset charset) {
        this(DEFAULT_SUFFIX, charset);
    }

    public CharsetResourceBundleControl() {
        this(DEFAULT_SUFFIX, StandardCharsets.UTF_8);
    }

    @Override
    public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
            throws IOException {

        String         bundleName   = toBundleName(baseName, locale);
        String         resourceName = toResourceName(bundleName, suffix);
        ResourceBundle bundle       = null;
        InputStream    stream       = null;

        if (reload) {
            URL url = loader.getResource(resourceName);
            if (url != null) {
                URLConnection connection = url.openConnection();
                if (connection != null) {
                    connection.setUseCaches(false);
                    stream = connection.getInputStream();
                }
            }
        } else {
            stream = loader.getResourceAsStream(resourceName);
        }

        if (stream != null) {
            try {
                bundle = new PropertyResourceBundle(new InputStreamReader(stream, charset));
            } finally {
                stream.close();
            }
        }

        return bundle;
    }
}
