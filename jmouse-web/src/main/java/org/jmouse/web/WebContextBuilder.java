package org.jmouse.web;

import org.jmouse.beans.BeanContextInitializer;
import org.jmouse.web.context.WebBeanContext;

import java.util.function.Consumer;

/**
 * 🧰 Fluent builder for constructing {@link WebBeanContext} instances.
 *
 * <p>Allows configuring context identity, scan roots, parent linkage,
 * default/framework initializers, Web MVC support, and last-mile customization.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * WebBeanContext ctx = new WebApplicationContextBuilder(factory)
 *     .name("web-app")
 *     .baseClasses(AppConfig.class, Controllers.class) // user classes
 *     .coreClasses(MyFrameworkDefaults.class)          // framework/sourceRoot scan (optional)
 *     .useDefault()                                    // scanners, env, etc.
 *     .useWebMvc()                                     // MVC infra + controllers
 *     .customize(c -> {/* post-creation tweaks *\/})
 *     .build();
 * }</pre>
 */
public interface WebContextBuilder {

    /**
     * 🏷 Set the logical ID/name of the context.
     *
     * @param contextId unique context identifier
     * @return this builder
     */
    WebContextBuilder name(String contextId);

    /**
     * 📦 Add application (user) classes as scan roots.
     * Typically where your controllers/config live.
     *
     * @param baseClasses classes whose packages will be scanned
     * @return this builder
     */
    WebContextBuilder userClasses(Class<?>... baseClasses);

    /**
     * 🧱 Add framework/sourceRoot classes as scan roots.
     * Use for infrastructure defaults that should be available to all apps.
     *
     * @param coreClasses classes whose packages will be scanned as core
     * @return this builder
     */
    WebContextBuilder coreClasses(Class<?>... coreClasses);

    /**
     * 🔗 Attach a parent context to enable hierarchical bean resolution.
     *
     * @param parent parent {@link WebBeanContext}
     * @return this builder
     */
    WebContextBuilder parent(WebBeanContext parent);

    /**
     * ➕ Register an extra initializer to run before refresh.
     *
     * @param initializer custom initializer
     * @return this builder
     */
    WebContextBuilder addInitializer(BeanContextInitializer initializer);

    /**
     * ✅ Enable default initializers (e.g., annotated scanning, environment setup).
     *
     * @return this builder
     */
    WebContextBuilder useDefault();

    /**
     * 🌐 Enable Web MVC setup (infrastructure + controllers mapping).
     *
     * @return this builder
     */
    WebContextBuilder useWebMvc();

    /**
     * 🛠 Provide a hook to customize the context after creation but before refresh.
     *
     * @param customizer consumer receiving the context for final adjustments
     * @return this builder
     */
    WebContextBuilder customize(Consumer<WebBeanContext> customizer);

    /**
     * 🚀 Build, initialize, and refresh the {@link WebBeanContext}.
     *
     * @return a fully initialized web bean context
     */
    WebBeanContext build();
}
