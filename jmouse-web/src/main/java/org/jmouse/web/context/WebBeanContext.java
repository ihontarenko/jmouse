package org.jmouse.web.context;

import jakarta.servlet.ServletContext;
import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.context.ApplicationBeanContext;
import org.jmouse.beans.BeanContext;
import org.jmouse.core.reflection.MethodFinder;
import org.jmouse.core.reflection.MethodMatchers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * 🌐 Web-specific {@link BeanContext}.
 * <p>
 * Adds servlet-related functionality and scoped beans for HTTP/web layers.
 * </p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
public interface WebBeanContext extends ApplicationBeanContext {

    /**
     * 🌍 Name of root web context.
     */
    String DEFAULT_ROOT_WEB_CONTEXT_NAME = "ROOT-CONTEXT";

    /**
     * 🧩 Name of child web context.
     */
    String DEFAULT_WEB_CONTEXT_NAME = "applicationContext";

    /**
     * 🧩 Name of child application classes.
     */
    String DEFAULT_APPLICATION_CLASSES_BEAN = "applicationClasses";

    /**
     * 🪝 Servlet attribute key to store root context.
     */
    String ROOT_WEB_BEAN_CONTEXT_ATTRIBUTE = WebBeanContext.class.getName() + ".ROOT";

    /**
     * 🔗 Servlet attribute key to bind request to context.
     */
    String CURRENT_REQUEST = WebBeanContext.class.getName() + ".CURRENT_REQUEST";

    /**
     * 🔗 Servlet attribute key to bind response to context.
     */
    String CURRENT_RESPONSE = WebBeanContext.class.getName() + ".CURRENT_RESPONSE";

    /**
     * 🔧 Attach {@link ServletContext}.
     *
     * @param servletContext current servlet context
     */
    void setServletContext(ServletContext servletContext);

    /**
     * 📥 Get the attached {@link ServletContext}.
     *
     * @return servlet context instance
     */
    ServletContext getServletContext();

    /**
     * 🚨 Get required root web context or throw.
     */
    static WebBeanContext getRequiredWebBeanContext(ServletContext servletContext) {
        WebBeanContext rootContext = getRootWebBeanContext(servletContext);
        if (rootContext == null) {
            throw new WebContextException("No WebBeanContext[%s] found.".formatted(DEFAULT_ROOT_WEB_CONTEXT_NAME));
        }
        return rootContext;
    }

    /**
     * 🧱 Get root context by known attribute name.
     */
    static WebBeanContext getRootWebBeanContext(ServletContext servletContext) {
        return (WebBeanContext) servletContext.getAttribute(ROOT_WEB_BEAN_CONTEXT_ATTRIBUTE);
    }

    /**
     * 🔍 Try to find any {@link WebBeanContext} within attributes.
     */
    static WebBeanContext findWebBeanContext(ServletContext servletContext) {
        WebBeanContext webBeanContext = getRootWebBeanContext(servletContext);

        if (webBeanContext == null) {
            Enumeration<String> names = servletContext.getAttributeNames();
            while (names.hasMoreElements()) {
                String name      = names.nextElement();
                Object attribute = servletContext.getAttribute(name);

                if (attribute instanceof WebBeanContext context) {
                    webBeanContext = context;
                    break;
                }
            }
        }

        return webBeanContext;
    }

    /**
     * 📦 Retrieves beans by type from the specified {@link WebBeanContext}.
     *
     * @param type    the bean type
     * @param context the web context
     * @return list of beans of the given type
     */
    static <T> List<T> getBeansOfType(Class<T> type, WebBeanContext context) {
        return context.getBeans(type);
    }

    /**
     * 📦 Retrieves beans by type from a {@link ServletContext}.
     *
     * @param type    the bean type
     * @param context the servlet context
     * @return list of beans of the given type
     */
    static <T> List<T> getBeansOfType(Class<T> type, ServletContext context) {
        return getBeansOfType(type, getRequiredWebBeanContext(context));
    }

    /**
     * 🔍 Finds local beans (not inherited) by type.
     *
     * @param type    the bean type
     * @param context the web context
     * @return map of local bean names to instances
     */
    static <T> Map<String, T> getLocalBeansOfType(Class<T> type, WebBeanContext context) {
        Map<String, T> beans = context.getBeansOfType(type);
        Map<String, T> local = new LinkedHashMap<>(4);

        beans.forEach((name, bean) -> {
            if (context.isLocalBean(name)) {
                local.put(name, bean);
            }
        });

        return local;
    }

    /**
     * 🧹 Shortcut to get local beans as a list.
     *
     * @param type    the bean type
     * @param context the web context
     * @return list of local beans
     */
    static <T> List<T> getLocalBeans(Class<T> type, WebBeanContext context) {
        return List.copyOf(getLocalBeansOfType(type, context).values());
    }

    /**
     * 🔍 Selects methods annotated with the given annotation from local beans.
     *
     * <p>Iterates over all local beans and applies the given {@link BiConsumer} to
     * methods matching the provided annotation type.
     *
     * @param annotationType the annotation to look for on methods
     * @param consumer       the consumer to handle found methods and their owning bean
     * @param context        the current web context
     */
    static void methodsOfAnnotatedClasses(Class<? extends Annotation> annotationType,
                                          BiConsumer<Collection<Method>, Object> consumer, WebBeanContext context) {
        for (String beanName : context.getBeanNames(Object.class)) {
            if (context.isLocalBean(beanName)) {
                BeanDefinition definition = context.getDefinition(beanName);
                if (definition.isAnnotatedWith(annotationType)) {
                    Object             bean    = context.getBean(definition.getBeanName());
                    Collection<Method> methods = new MethodFinder()
                            .find(definition.getBeanClass(), MethodMatchers.isPublic());
                    consumer.accept(methods, bean);
                }
            }
        }
    }

}
