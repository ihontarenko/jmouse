package app.api;

import email.app.EmailCfg;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.BeanScan;
import org.jmouse.core.MediaType;
import org.jmouse.mvc.HandlerMapping;
import org.jmouse.mvc.Route;
import org.jmouse.mvc.RouteMatch;
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
    public ControllerMethodRegistration route1Registration() {
        Route route = Route.route()
                .GET("/index")
                .header(HttpHeader.CONTENT_MD5, "123qwe")
                .produces(MediaType.APPLICATION_JSON)
                .build();

        return new ControllerMethodRegistration(route, (request, response)
                -> response.getWriter().write("[{\"name\":\"Route 1\"}]"));
    }

    @Bean
    public ControllerMethodRegistration route2Registration() {
        Route route = Route.route()
                .GET("/index")
                .produces(MediaType.APPLICATION_JSON)
                .build();

        return new ControllerMethodRegistration(route, (request, response)
                -> response.getWriter().write("[{'name':'Route 2'}]"));
    }

    @Bean
    public ControllerMethodRegistration route3Registration() {
        Route route = Route.route()
                .GET("/index")
                .produces(MediaType.APPLICATION_XML)
                .build();

        return new ControllerMethodRegistration(route, (request, response)
                -> response.getWriter().write("[{'name':'Route 3'}]"));
    }

    @Bean
    public ControllerMethodRegistration route4Registration() {
        Route route = Route.route()
                .GET("/index")
                .build();

        return new ControllerMethodRegistration(route, (request, response)
                -> response.getWriter().write("[{'name':'Route 4'}]"));
    }

    @Bean
    public ControllerMethodRegistration route5Registration() {
        Route route = Route.route()
                .GET("/index")
                .produces(MediaType.TEXT_HTML)
                .build();

        return new ControllerMethodRegistration(route, (request, response)
                -> response.getWriter().write("[{'name':'Route 5'}]"));
    }

    @Bean
    public ControllerMethodRegistration route6Registration() {
        Route route = Route.route()
                .GET("/index")
                .produces(MediaType.TEXT_HTML)
                .header(HttpHeader.ETAG, "Tag!")
                .build();

        return new ControllerMethodRegistration(route, (request, response)
                -> response.getWriter().write("[{'name':'Route 6'}]"));
    }

    @Bean
    public ControllerMethodRegistration route7Registration() {
        Route route = Route.route()
                .GET("/index")
                .produces(MediaType.TEXT_HTML)
                .queryParameter("lang", "uk")
                .build();

        return new ControllerMethodRegistration(route, (request, response)
                -> response.getWriter().write("[{'name':'Route 7'}]"));
    }

    @Bean
    public ControllerMethodRegistration route8Registration() {
        Route route = Route.route()
                .GET("/index")
                .produces(MediaType.TEXT_HTML)
                .header(HttpHeader.ACCEPT_LANGUAGE, "uk")
                .queryParameter("lang", "uk")
                .build();

        return new ControllerMethodRegistration(route, (request, response)
                -> response.getWriter().write("[{'name':'Route 8'}]"));
    }

    @Bean
    public ControllerMethodRegistration helloRegistration(WebBeanContext webBeanContext) {
        return new ControllerMethodRegistration(Route.GET("/hello/{id:int}/{active:boolean}"), (request, response) -> {
            RouteMatch routePath = (RouteMatch) request.getAttribute(HandlerMapping.ROUTE_MATCH_ATTRIBUTE);
            Writer     writer    = response.getWriter();

//            webBeanContext.getBean(Model.class);

            writer.write("<h1>Hello! ID: %s</h1>".formatted(routePath.variables().get("id")));
        });
    }

}
