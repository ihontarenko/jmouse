package org.jmouse.web_app;

import org.jmouse.jdbc.EnableJdbc;
import org.jmouse.jdbc.connection.datasource.DataSourceKeyHolder;
import org.jmouse.security.web.EnableAuthorizeMethod;
import org.jmouse.security.web.EnableJsr250MethodSecurity;
import org.jmouse.security.web.EnableWebSecurity;
import org.jmouse.web.mvc.WebApplicationLauncher;

@EnableJdbc
@EnableWebSecurity
@EnableJsr250MethodSecurity
@EnableAuthorizeMethod
public class Application {

    static void main(String[] arguments) {
        DataSourceKeyHolder.use("mysql");
        new WebApplicationLauncher(Application.class).launch(arguments);
    }

}

