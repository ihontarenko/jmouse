package app.api;

import email.app.EmailCfg;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.BeanScan;
import org.jmouse.mvc.HandlerMapping;
import org.jmouse.mvc.RoutePath;
import org.jmouse.mvc.mapping.RouteRegistration;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.request.http.HttpMethod;

import java.io.Writer;

@BeanFactories
@BeanScan(EmailCfg.class)
public class WebConfig {

    @Bean
    public RouteRegistration cfgRegistration() {
        return new RouteRegistration(HttpMethod.GET, "/cfg", (request, response)
                -> response.getWriter().write("web_config"));
    }

    @Bean
    public RouteRegistration helloRegistration(WebBeanContext webBeanContext) {
        return new RouteRegistration(HttpMethod.GET, "/hello/{id:int}/{active:boolean}", (request, response) -> {
            RoutePath routePath = (RoutePath) request.getAttribute(HandlerMapping.ROUTE_PATH_ATTRIBUTE);
            Writer    writer    = response.getWriter();

//            webBeanContext.getBean(Model.class);

            writer.write("<h1>Hello! ID: %s</h1>".formatted(routePath.variables().get("id")));
        });
    }

}
