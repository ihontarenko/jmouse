package email.app;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.mvc.mapping.RouteRegistration;
import org.jmouse.web.request.http.HttpMethod;

@BeanFactories
public class EmailCfg {

    @Bean("email")
    public String getName() {
        return getClass().getName();
    }

    @Bean
    public RouteRegistration cfgRegistration() {
        return new RouteRegistration(HttpMethod.GET, "/email", (request, response)
                -> response.getWriter().write("email_config"));
    }

}
