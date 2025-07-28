package app.api.config;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.mvc.mapping.ControllerRegistration;

@BeanFactories
public class ApiConfig {

    @Bean
    public ControllerRegistration appRegistration() {
        return new ControllerRegistration("/app", (request, response)
                -> response.getWriter().write("app_api"));
    }

    @Bean
    public ControllerRegistration webRegistration() {
        return new ControllerRegistration("/web", (request, response)
                -> response.getWriter().write("app_api_web"));
    }

    @Bean
    public ControllerRegistration endRegistration() {
        return new ControllerRegistration("/end", (request, response)
                -> response.getWriter().write("end!!!"));
    }

    @Bean
    public ControllerRegistration userIdRegistration() {
        return new ControllerRegistration("/user/{id}/{status:REGISTERED|BLOCKED}", (request, response)
                -> response.getWriter().write("user id"));
    }

}
