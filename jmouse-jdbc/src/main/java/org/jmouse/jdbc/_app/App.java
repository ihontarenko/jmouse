package org.jmouse.jdbc._app;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.jdbc.connection.UserPasswordConnectionConfiguration;

@BeanFactories
public class App {

    @Bean
    public UserPasswordConnectionConfiguration userPasswordConnectionConfiguration() {
        return new UserPasswordConnectionConfiguration(
                "jdbc:mysql://localhost:3306/jmouse?useSSL=false&allowPublicKeyRetrieval=true",
                "jmouse", "jmouse"
        );
    }

}
