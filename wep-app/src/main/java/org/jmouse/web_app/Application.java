package org.jmouse.web_app;

import org.jmouse.beans.annotation.BeanScan;
import org.jmouse.security.web.MatchableSecurityFilterChain;
import org.jmouse.web.mvc.WebApplicationLauncher;

@BeanScan(MatchableSecurityFilterChain.class)
public class Application {

    public static void main(String[] arguments) {
        new WebApplicationLauncher(Application.class).launch(arguments);
    }

}
