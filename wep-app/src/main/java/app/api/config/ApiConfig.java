package app.api.config;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.web.http.RequestAttributesHolder;
import org.jmouse.web.match.Route;
import org.jmouse.web.match.RouteMatch;
import org.jmouse.web.mvc.HandlerMapping;
import org.jmouse.web.mvc.mapping.RequestHttpHandlerRegistration;

@BeanFactories
public class ApiConfig {

    @Bean
    public RequestHttpHandlerRegistration appRegistration() {
        return new RequestHttpHandlerRegistration(Route.GET("/app"), (request, response)
                -> response.getWriter().write("app_api"));
    }

    @Bean
    public RequestHttpHandlerRegistration webRegistration() {
        return new RequestHttpHandlerRegistration(Route.GET("/web"), (request, response)
                -> response.getWriter().write("app_api_web"));
    }

    @Bean
    public RequestHttpHandlerRegistration endRegistration() {
        return new RequestHttpHandlerRegistration(Route.GET("/end"), (request, response)
                -> response.getWriter().write("end!!!"));
    }

    @Bean
    public RequestHttpHandlerRegistration userIdRegistration() {
        return new RequestHttpHandlerRegistration(Route.GET("/user/{id}/{status:REGISTERED|BLOCKED}"), (request, response) -> {
            System.out.println(request);
            RouteMatch match = (RouteMatch) RequestAttributesHolder.getAttribute(HandlerMapping.ROUTE_MATCH_ATTRIBUTE);
            response.getWriter().write(STR."user id \{match.variables().get("id")}");
        });
    }

}
