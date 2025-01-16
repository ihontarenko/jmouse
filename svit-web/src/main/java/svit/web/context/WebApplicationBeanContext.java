package svit.web.context;

import svit.beans.BeanContext;
import svit.beans.BeanScope;
import svit.beans.container.ThreadLocalBeanContainer;
import svit.beans.container.ThreadLocalScope;

public class WebApplicationBeanContext extends AbstractApplicationContext {

    public WebApplicationBeanContext(BeanContext parent) {
        super(parent);

        registerBeanContainer(ThreadLocalScope.THREAD_LOCAL_SCOPE, new ThreadLocalBeanContainer());
        registerBeanContainer(BeanScope.NON_BEAN, this);
    }

    public WebApplicationBeanContext(Class<?>... baseClasses) {
        super(baseClasses);
    }
}
