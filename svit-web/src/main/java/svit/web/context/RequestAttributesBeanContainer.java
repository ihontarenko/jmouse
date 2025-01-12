package svit.web.context;

import svit.beans.BeanInstanceContainer;
import svit.beans.ObjectFactory;
import svit.web.context.request.RequestAttributes;

abstract public class RequestAttributesBeanContainer implements BeanInstanceContainer {

    private final ObjectFactory<RequestAttributes> objectFactory;

    protected RequestAttributesBeanContainer(ObjectFactory<RequestAttributes> objectFactory) {
        this.objectFactory = objectFactory;
    }

    @Override
    public <T> T getBean(String name) {
        return (T) objectFactory.createObject().getAttribute(name);
    }

    @Override
    public void registerBean(String name, Object bean) {
        objectFactory.createObject().setAttribute(name, bean);
    }

}
