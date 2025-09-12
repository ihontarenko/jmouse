package org.jmouse.web.mvc.view.internal;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.el.extension.Extension;

/**
 * ⚙️ Internal configuration for jMouse view layer.
 *
 * <p>Registers built-in {@link Extension}s that provide
 * expression language (EL) functions for server-side templates.</p>
 *
 * <p>💡 This configuration is automatically loaded via {@link BeanFactories}.</p>
 */
@BeanFactories
public class InternalViewConfiguration {

    /**
     * 🧩 Register the core {@link InternalViewExtension}.
     *
     * <p>Provides functions such as {@code jMouseAsset()} for resolving
     * asset URLs inside views.</p>
     *
     * @return {@link Extension} with built-in view functions
     */
    @Bean
    public Extension jMouseInternalViewExtension() {
        return new InternalViewExtension();
    }
}
