package app.api.config;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.web.mvc.Route;
import org.jmouse.web.mvc.mapping.ControllerMethodRegistration;

@BeanFactories
public class ApiConfig {

    @Bean
    public ControllerMethodRegistration appRegistration() {
        return new ControllerMethodRegistration(Route.GET("/app"), (request, response)
                -> response.getWriter().write("app_api"));
    }

    @Bean
    public ControllerMethodRegistration webRegistration() {
        return new ControllerMethodRegistration(Route.GET("/web"), (request, response)
                -> response.getWriter().write("app_api_web"));
    }

    @Bean
    public ControllerMethodRegistration endRegistration() {
        return new ControllerMethodRegistration(Route.GET("/end"), (request, response)
                -> response.getWriter().write("end!!!"));
    }

    @Bean
    public ControllerMethodRegistration userIdRegistration() {
        return new ControllerMethodRegistration(Route.GET("/user/{id}/{status:REGISTERED|BLOCKED}"), (request, response)
                -> response.getWriter().write("user id"));
    }

}
