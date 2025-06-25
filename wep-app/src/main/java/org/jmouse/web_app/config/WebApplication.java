package org.jmouse.web_app.config;

import org.jmouse.beans.annotation.Factories;
import org.jmouse.beans.annotation.Provide;
import org.jmouse.mvc.WebApplicationLauncher;
import org.jmouse.mvc._test.Service;

@Factories
public class WebApplication {

    public static void main(String[] arguments) {
        new WebApplicationLauncher(WebApplication.class).launch();
    }

    @Provide
    public Service getService() {
        return new Service("local_app");
    }

}
