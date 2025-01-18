package svit.web.context;

import jakarta.servlet.ServletContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svit.beans.BeanContext;
import svit.beans.DefaultBeanContext;

import static svit.reflection.Reflections.getShortName;

public class AbstractWebApplicationBeanContext extends DefaultBeanContext implements WebBeanContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractWebApplicationBeanContext.class);

    public AbstractWebApplicationBeanContext(BeanContext parent) {
        super(parent);
    }

    public AbstractWebApplicationBeanContext(Class<?>... baseClasses) {
        super(baseClasses);
    }

    @Override
    public ServletContext getServletContext() {
        return getBean(ServletContext.class);
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        LOGGER.info("Attach servlet context '{}'", getShortName(servletContext.getClass()));
        registerBean(ServletContext.class, servletContext);
    }

}
