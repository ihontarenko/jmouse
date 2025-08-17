package org.jmouse.web_app;

import org.jmouse.web.mvc.WebApplicationLauncher;

public class Application {

    public static void main(String[] arguments) {
        new WebApplicationLauncher(Application.class).launch(arguments);
    }

}
