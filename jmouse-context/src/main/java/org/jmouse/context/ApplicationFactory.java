package org.jmouse.context;

import svit.beans.BeanContext;
import org.jmouse.core.env.Environment;

public interface ApplicationFactory<T extends BeanContext> extends ApplicationContextFactory<T> {

    Environment createDefaultEnvironment();

}
