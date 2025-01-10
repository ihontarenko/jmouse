package svit.web.context;

import jakarta.servlet.http.HttpSession;
import svit.container.BeanInstanceContainer;
import svit.container.ObjectFactory;

public class HttpSessionBeanContainer implements BeanInstanceContainer {

    private final ObjectFactory<HttpSession> objectFactory;


    public HttpSessionBeanContainer(ObjectFactory<HttpSession> objectFactory) {
        this.objectFactory = objectFactory;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) {
        return (T) objectFactory.createObject().getAttribute(name);
    }


    @Override
    public void registerBean(String name, Object bean) {
        objectFactory.createObject().setAttribute(name, bean);
    }
}
