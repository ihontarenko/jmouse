package org.jmouse.web.context;

import svit.beans.BeanContext;
import svit.beans.container.ThreadLocalBeanContainer;
import svit.beans.container.ThreadLocalScope;

public class WebApplicationBeanContext extends AbstractWebApplicationBeanContext {

    public WebApplicationBeanContext(BeanContext parent, Class<?>... baseClasses) {
        super(parent);

        setBaseClasses(baseClasses);

        registerBeanContainer(ThreadLocalScope.THREAD_LOCAL_SCOPE, new ThreadLocalBeanContainer());
    }

    public WebApplicationBeanContext(Class<?>... baseClasses) {
        this(null, baseClasses);
    }


}
