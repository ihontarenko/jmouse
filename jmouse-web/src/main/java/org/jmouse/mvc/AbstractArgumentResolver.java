package org.jmouse.mvc;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.InitializingBean;
import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.core.convert.Conversion;
import org.jmouse.web.context.WebBeanContext;

/**
 * ⚙️ Base class for {@link ArgumentResolver} implementations with initialization hook.
 *
 * <p>Used as a view for resolvers that require access to the {@link WebBeanContext}
 * during the application context startup phase.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public abstract class AbstractArgumentResolver implements ArgumentResolver, InitializingBeanSupport<WebBeanContext> {

    protected Conversion conversion;

    /**
     * 🧩 Initialization entrypoint called after bean creation is complete.
     *
     * @param context the initialized {@link BeanContext}, expected to be a {@link WebBeanContext}
     */
    @Override
    public void afterCompletion(BeanContext context) {
        initialize((WebBeanContext) context);
    }

    /**
     * 🛠 Entry point for subclass-specific initialization logic.
     *
     * @param context the web-specific bean context
     */
    public void initialize(WebBeanContext context) {
        this.conversion = context.getBean(Conversion.class);
        doInitialize(context);
    }
}
