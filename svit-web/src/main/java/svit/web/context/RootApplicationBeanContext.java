package svit.web.context;

import svit.beans.BeanContext;

public class RootApplicationBeanContext extends WebApplicationBeanContext {

    public RootApplicationBeanContext(BeanContext parent) {
        super(parent);
    }

    public RootApplicationBeanContext(Class<?>... baseClasses) {
        super(baseClasses);
    }

}
