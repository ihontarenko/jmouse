package org.jmouse.web;

import svit.beans.BeanContext;

public interface Launcher<C extends BeanContext> {

    C launch(String... arguments);

}
