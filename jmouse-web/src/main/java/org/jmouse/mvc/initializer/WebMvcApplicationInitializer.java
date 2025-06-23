package org.jmouse.mvc.initializer;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.jmouse.beans.annotation.BeanConstructor;
import org.jmouse.mvc.BeanInstanceInitializer;
import org.jmouse.util.Priority;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.initializer.WebApplicationInitializer;

import java.util.List;

/**
 * 🚀 Runs all {@link BeanInstanceInitializer} beans on web startup.
 *
 * Scans the {@link WebBeanContext} for all registered MVC initializers and
 * applies them to corresponding beans (by type).
 *
 * 📌 Priority: 1 — runs latest in web initialization phase.
 *
 * Example use:
 * <pre>{@code
 * @Provide
 * public class HandlerMappingInitializer implements WebMvcInitializer<HandlerMapping> {
 *     public void initialize(HandlerMapping mapping) { ... }
 *     public Class<HandlerMapping> objectClass() { return HandlerMapping.class; }
 * }
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
@Priority(1)
public class WebMvcApplicationInitializer implements WebApplicationInitializer {

    private final WebBeanContext context;

    /**
     * 💡 Inject Web context for scanning beans.
     *
     * @param context active web bean context
     */
    @BeanConstructor
    public WebMvcApplicationInitializer(WebBeanContext context) {
        this.context = context;
    }

    /**
     * ⚙️ Startup hook — initializes all applicable MVC beans using their registered {@link BeanInstanceInitializer}.
     *
     * @param servletContext current servlet context
     */
    @Override
    @SuppressWarnings({"all"})
    public void onStartup(ServletContext servletContext) {
        for (BeanInstanceInitializer beanInitializer : context.getBeans(BeanInstanceInitializer.class)) {
            Class<?> beanClass = beanInitializer.objectClass();
            if (beanClass != Object.class) {
                List<Object> beans = context.getBeans((Class<Object>) beanClass);
                for (Object bean : beans) {
                    beanInitializer.initialize(bean);
                }
            }
        }
    }
}
