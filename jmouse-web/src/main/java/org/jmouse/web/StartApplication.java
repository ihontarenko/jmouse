package org.jmouse.web;

import org.jmouse.WebApplicationLauncher;
import org.jmouse.context.ApplicationBeanContext;

public class StartApplication {

    public static void main(String... arguments) {
        new WebApplicationLauncher(StartApplication.class, ApplicationBeanContext.class).launch();
    }

}
