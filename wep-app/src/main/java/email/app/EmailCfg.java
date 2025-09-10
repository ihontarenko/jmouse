package email.app;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.web.mvc.Route;
import org.jmouse.web.mvc.mapping.RequestHttpHandlerRegistration;

@BeanFactories
public class EmailCfg {

    @Bean("email")
    public String getName() {
        return getClass().getName();
    }

    @Bean
    public RequestHttpHandlerRegistration cfgRegistration() {
        return new RequestHttpHandlerRegistration(Route.GET("/email"), (request, response)
                -> response.getWriter().write("email_config"));
    }

}
