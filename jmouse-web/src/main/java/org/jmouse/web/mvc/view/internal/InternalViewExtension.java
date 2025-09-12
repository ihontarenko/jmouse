package org.jmouse.web.mvc.view.internal;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanContextAware;
import org.jmouse.el.extension.Extension;
import org.jmouse.el.extension.Function;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.mvc.view.internal.extension.jMouseAssetsFunction;

import java.util.List;

/**
 * 🧩 Internal view extension for the jMouse MVC framework.
 *
 * <p>Registers built-in expression language (EL) functions
 * used in server-side view rendering.</p>
 *
 * <p>💡 Currently provides {@link jMouseAssetsFunction} for
 * resolving asset URLs within templates.</p>
 */
public class InternalViewExtension implements Extension, BeanContextAware {

    /**
     * 🌐 Current web bean context injected by the framework.
     */
    private WebBeanContext context;

    /**
     * 📑 Return the list of EL functions exposed by this extension.
     *
     * @return immutable list of functions (currently only {@link jMouseAssetsFunction})
     */
    @Override
    public List<Function> getFunctions() {
        return List.of(new jMouseAssetsFunction(context));
    }

    /**
     * 🌐 Return the current {@link BeanContext}.
     *
     * @return active web bean context
     */
    @Override
    public BeanContext getBeanContext() {
        return context;
    }

    /**
     * 🔧 Inject the active {@link BeanContext}.
     *
     * @param context application bean context (cast to {@link WebBeanContext})
     */
    @Override
    public void setBeanContext(BeanContext context) {
        this.context = (WebBeanContext) context;
    }
}
