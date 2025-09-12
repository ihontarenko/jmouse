package org.jmouse.web.mvc.view.internal.extension;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Function;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.mvc.resource.ResourceUrlResolver;

/**
 * 🎨 Expression Language (EL) function for resolving asset URLs.
 *
 * <p>Delegates to {@link ResourceUrlResolver} to transform a given
 * asset path (e.g. {@code "css/app.css"}) into a versioned,
 * cache-busting URL.</p>
 *
 * <p>💡 Available to templates as: <pre>{@code ${jMouseAsset("css/app.css")}}</pre></p>
 */
public class jMouseAssetsFunction implements Function {

    /**
     * 🌐 Current web context for bean resolution.
     */
    private final WebBeanContext context;

    /**
     * 🔗 Cached {@link ResourceUrlResolver} for URL lookups.
     */
    private ResourceUrlResolver resolver;

    /**
     * 🏗️ Create a new asset function with the given web context.
     *
     * @param context active {@link WebBeanContext}
     */
    public jMouseAssetsFunction(WebBeanContext context) {
        this.context = context;
    }

    /**
     * ▶️ Execute the function with provided arguments.
     *
     * <p>Expects the first argument to be a {@link String} asset path,
     * resolves it via {@link ResourceUrlResolver#lookupResourceUrl(String)}.</p>
     *
     * @param arguments function arguments
     * @param context   evaluation context (not used here)
     * @return resolved URL string, or {@code null} if input was not a string
     */
    @Override
    public Object execute(Arguments arguments, EvaluationContext context) {
        Object asset = arguments.getFirst();

        if (asset instanceof String path) {
            return getResolver().lookupResourceUrl(path);
        }

        return null;
    }

    /**
     * 🏷️ Return the function name exposed to templates.
     *
     * @return {@code "jMouseAsset"}
     */
    @Override
    public String getName() {
        return "jMouseAsset";
    }

    /**
     * 🔧 Lazily resolve and cache the {@link ResourceUrlResolver}.
     *
     * @return resolver bean from the context
     */
    private ResourceUrlResolver getResolver() {
        ResourceUrlResolver resolver = this.resolver;

        if (resolver == null) {
            resolver = context.getBean(ResourceUrlResolver.class);
            this.resolver = resolver;
        }

        return resolver;
    }
}
