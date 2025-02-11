package org.jmouse.web.context;

import org.jmouse.testing_ground.beans.BeanContext;
import org.jmouse.testing_ground.beans.container.ThreadLocalBeanContainer;
import org.jmouse.testing_ground.beans.container.ThreadLocalScope;

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
