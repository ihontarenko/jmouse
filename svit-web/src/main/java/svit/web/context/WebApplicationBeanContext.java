package svit.web.context;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svit.container.*;
import svit.container.definition.BeanDefinition;
import svit.web.context.support.HttpSessionObjectFactory;
import svit.web.context.support.RequestObjectFactory;

import static svit.reflection.Reflections.getShortName;

public class WebApplicationBeanContext extends DefaultBeanContext implements WebBeanContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebApplicationBeanContext.class);

    private final BeanInstanceContainer sessionContainer;
    private final BeanInstanceContainer requestContainer;
    private       ServletContext        servletContext;

    public WebApplicationBeanContext(BeanContext parent) {
        super(parent);

        ObjectFactory<ServletContext>     factory        = this::getServletContext;
        ObjectFactory<HttpServletRequest> requestFactory = new RequestObjectFactory(factory);

        sessionContainer = new HttpSessionBeanContainer(new HttpSessionObjectFactory(requestFactory));
        requestContainer = new HttpRequestBeanContainer(requestFactory);
    }

    public WebApplicationBeanContext() {
        this(null);
    }

    @Override
    public <T> T getBean(String name) {
        BeanDefinition   definition    = getDefinition(name);
        ObjectFactory<T> objectFactory = () -> super.createBean(definition);

        if (definition == null) {
            throw new BeanContextException("No registered bean found with the name '%s'.".formatted(name));
        }

        return switch (definition.getBeanScope()) {
            case SESSION -> sessionContainer.getBean(name, objectFactory);
            case REQUEST -> requestContainer.getBean(name, objectFactory);
            default -> super.getBean(name);
        };
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
        LOGGER.info("Register bean '{}'", getShortName(ServletContext.class));
        registerBean(ServletContext.class, servletContext);
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

}
