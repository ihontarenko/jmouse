package email.app;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.mvc.Route;
import org.jmouse.mvc.mapping.ControllerMethodRegistration;

@BeanFactories
public class EmailCfg {

    @Bean("email")
    public String getName() {
        return getClass().getName();
    }

    @Bean
    public ControllerMethodRegistration cfgRegistration() {
        return new ControllerMethodRegistration(Route.GET("/email"), (request, response)
                -> response.getWriter().write("email_config"));
    }

}
