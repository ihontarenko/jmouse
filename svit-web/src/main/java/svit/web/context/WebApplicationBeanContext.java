package svit.web.context;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svit.beans.BeanContext;
import svit.beans.BeanScope;
import svit.beans.DefaultBeanContext;
import svit.beans.ObjectFactory;
import svit.web.context.support.HttpSessionObjectFactory;
import svit.web.context.support.RequestObjectFactory;

import static svit.reflection.Reflections.getShortName;

public class WebApplicationBeanContext extends DefaultBeanContext implements WebBeanContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebApplicationBeanContext.class);

    private ServletContext servletContext;

    public WebApplicationBeanContext(BeanContext parent) {
        super(parent);

        ObjectFactory<ServletContext>     factory        = this::getServletContext;
        ObjectFactory<HttpServletRequest> requestFactory = new RequestObjectFactory(factory);

        registerBeanContainer(BeanScope.REQUEST, new HttpRequestBeanContainer(requestFactory));
        registerBeanContainer(BeanScope.SESSION, new HttpSessionBeanContainer(new HttpSessionObjectFactory(requestFactory)));
        registerBeanContainer(BeanScope.NON_BEAN, this);
    }

    public WebApplicationBeanContext() {
        this(null);
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
        LOGGER.info("Register bean '{}'", getShortName(ServletContext.class));
        registerBean(ServletContext.class, servletContext);
    }

}
