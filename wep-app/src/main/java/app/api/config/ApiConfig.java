package app.api.config;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.mvc.mapping.RouteRegistration;
import org.jmouse.web.request.http.HttpMethod;

@BeanFactories
public class ApiConfig {

    @Bean
    public RouteRegistration appRegistration() {
        return new RouteRegistration(HttpMethod.GET, "/app", (request, response)
                -> response.getWriter().write("app_api"));
    }

    @Bean
    public RouteRegistration webRegistration() {
        return new RouteRegistration(HttpMethod.GET, "/web", (request, response)
                -> response.getWriter().write("app_api_web"));
    }

    @Bean
    public RouteRegistration endRegistration() {
        return new RouteRegistration(HttpMethod.GET, "/end", (request, response)
                -> response.getWriter().write("end!!!"));
    }

    @Bean
    public RouteRegistration userIdRegistration() {
        return new RouteRegistration(HttpMethod.GET, "/user/{id}/{status:REGISTERED|BLOCKED}", (request, response)
                -> response.getWriter().write("user id"));
    }

}
