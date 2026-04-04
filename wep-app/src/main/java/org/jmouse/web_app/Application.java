package org.jmouse.web_app;

import org.jmouse.jdbc.EnableJdbc;
import org.jmouse.jdbc.connection.datasource.DataSourceKeyHolder;
import org.jmouse.security.web.EnableWebSecurity;
import org.jmouse.web.mvc.WebApplicationLauncher;

@EnableWebSecurity
@EnableJdbc
public class Application {

    static void main(String[] arguments) {
        DataSourceKeyHolder.use("mysql");
        new WebApplicationLauncher(Application.class).launch(arguments);
    }

}
