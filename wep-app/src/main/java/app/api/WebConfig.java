package app.api;

import email.app.EmailCfg;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.BeanScan;
import org.jmouse.core.MediaType;
import org.jmouse.mvc.HandlerMapping;
import org.jmouse.mvc.Route;
import org.jmouse.mvc.RoutePath;
import org.jmouse.mvc.mapping.ControllerMethodRegistration;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.request.http.HttpHeader;

import java.io.Writer;

@BeanFactories
@BeanScan(EmailCfg.class)
public class WebConfig {

    @Bean
    public ControllerMethodRegistration cfgRegistration() {
        return new ControllerMethodRegistration(Route.GET("/cfg"), (request, response)
                -> response.getWriter().write("web_config"));
    }

    @Bean
    public ControllerMethodRegistration indexRegistration() {
        Route route = Route.route()
                .GET("/index")
                .header(HttpHeader.CONTENT_MD5, "123qwe")
                .produces(MediaType.APPLICATION_JSON)
                .build();

        return new ControllerMethodRegistration(route, (request, response)
                -> response.getWriter().write("[{'name':'Ivan'}]"));
    }

    @Bean
    public ControllerMethodRegistration helloRegistration(WebBeanContext webBeanContext) {
        return new ControllerMethodRegistration(Route.GET("/hello/{id:int}/{active:boolean}"), (request, response) -> {
            RoutePath routePath = (RoutePath) request.getAttribute(HandlerMapping.ROUTE_PATH_ATTRIBUTE);
            Writer    writer    = response.getWriter();

//            webBeanContext.getBean(Model.class);

            writer.write("<h1>Hello! ID: %s</h1>".formatted(routePath.variables().get("id")));
        });
    }

}
