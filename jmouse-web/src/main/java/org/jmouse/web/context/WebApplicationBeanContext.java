package org.jmouse.web.context;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanScope;
import org.jmouse.beans.container.ThreadLocalBeanContainer;
import org.jmouse.beans.container.ThreadLocalScope;

public class WebApplicationBeanContext extends AbstractWebApplicationBeanContext {

    public WebApplicationBeanContext(BeanContext parent, Class<?>... baseClasses) {
        super(parent);

        setBaseClasses(baseClasses);

        registerBeanContainer(ThreadLocalScope.THREAD_LOCAL_SCOPE, new ThreadLocalBeanContainer());
        registerBeanContainer(BeanScope.REQUEST, new RequestScopedBeanContainer());
    }

    public WebApplicationBeanContext(Class<?>... baseClasses) {
        this(null, baseClasses);
    }


}
