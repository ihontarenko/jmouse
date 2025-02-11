package org.jmouse.core.i18n;

import org.jmouse.util.Charset;

import java.util.Locale;

/**
 * A standard implementation of a resource bundle loader.
 * <p>
 * This class extends {@link AbstractMessageSourceBundle} and is responsible for
 * loading message bundles using a specified {@link ClassLoader}. It also includes
 * a default JDK-based message bundle loader.
 * </p>
 */
public class StandardResourceBundleLoader extends AbstractMessageSourceBundle {

    /**
     * Constructs a new {@code StandardResourceBundleLoader} with the given class loader.
     * <p>
     * The class loader is used to locate and load resource bundles. Additionally,
     * a default JDK message bundle loader is registered.
     * </p>
     *
     * @param classLoader the class loader used to load message bundles
     */
    public StandardResourceBundleLoader(ClassLoader classLoader) {
        super(classLoader);

        setDefaultEncoding(Charset.ISO_8859_1);
        setDefaultLocale(Locale.getDefault());

        // Add a JDK-based message bundle loader to the list of loaders
        loaders.add(new JdkMessageBundle.Loader());
    }
}
