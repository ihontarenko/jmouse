package svit.web.context;

import jakarta.servlet.ServletContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svit.beans.BeanContext;
import svit.beans.DefaultBeanContext;
import svit.beans.container.ThreadLocalScope;

import static svit.reflection.Reflections.getShortName;

public class AbstractApplicationContext extends DefaultBeanContext implements ApplicationContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebApplicationBeanContext.class);

    private ServletContext servletContext;

    public AbstractApplicationContext(BeanContext parent) {
        super(parent);
    }

    public AbstractApplicationContext(Class<?>... baseClasses) {
        super(baseClasses);
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
        LOGGER.info("Register bean '{}'", getShortName(ServletContext.class));
        registerBean(ServletContext.class, servletContext, ThreadLocalScope.THREAD_LOCAL_SCOPE);
    }

}
