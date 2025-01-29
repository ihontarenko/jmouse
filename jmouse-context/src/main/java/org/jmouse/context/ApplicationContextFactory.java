package org.jmouse.context;

import svit.beans.BeanContext;

public interface ApplicationContextFactory<T extends BeanContext> {

    T createContext(String contextId, Class<?>... classes);

    T createContext(String contextId, T rootContext, Class<?>... classes);

}
