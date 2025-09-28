package app.api;

import email.app.EmailCfg;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.BeanScan;
import org.jmouse.core.MediaType;
import org.jmouse.web.mvc.NotFoundException;
import org.jmouse.web.match.Route;
import org.jmouse.web.mvc.mapping.RequestHttpHandlerRegistration;
import org.jmouse.web.http.HttpHeader;

@BeanFactories
@BeanScan(EmailCfg.class)
public class WebConfig {

    @Bean
    public RequestHttpHandlerRegistration errorRegistration() {
        return new RequestHttpHandlerRegistration(Route.GET("/error"), (request, response) -> {
            throw new NotFoundException("Test error page with NOT FOUND error!");
        });
    }

    @Bean
    public RequestHttpHandlerRegistration route8Registration() {
        Route route = Route.route()
                .GET("/index")
                .produces(MediaType.TEXT_HTML)
                .header(HttpHeader.ACCEPT_LANGUAGE, "uk")
                .queryParameter("lang", "uk")
                .build();

        return new RequestHttpHandlerRegistration(route, (request, response)
                -> response.getWriter().write("[{'name':'Route 8'}]"));
    }

}
