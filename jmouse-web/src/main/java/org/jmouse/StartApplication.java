package org.jmouse;

import org.jmouse.context.ApplicationBeanContext;
import org.jmouse.mvc.WebApplicationLauncher;

public class StartApplication {

    public static void main(String... arguments) {
        new WebApplicationLauncher(StartApplication.class, ApplicationBeanContext.class).launch();
    }

}
