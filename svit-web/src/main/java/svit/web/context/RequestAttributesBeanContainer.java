package svit.web.context;

import svit.beans.BeanInstanceContainer;
import svit.beans.ObjectFactory;
import svit.web.context.request.RequestAttributes;
import svit.web.context.request.RequestAttributesHolder;

abstract public class RequestAttributesBeanContainer implements BeanInstanceContainer {

    @Override
    public <T> T getBean(String name) {
        return (T) getRequestAttribute().getAttribute(name);
    }

    @Override
    public void registerBean(String name, Object bean) {
        getRequestAttribute().setAttribute(name, bean);
    }

    private RequestAttributes getRequestAttribute() {
        return RequestAttributesHolder.getRequestAttributes();
    }

}
