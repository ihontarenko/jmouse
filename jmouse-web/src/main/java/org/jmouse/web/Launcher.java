package org.jmouse.web;

import org.jmouse.beans.BeanContext;

public interface Launcher<C extends BeanContext> {

    C launch(String... arguments);

}
