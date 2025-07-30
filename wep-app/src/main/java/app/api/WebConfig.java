package app.api;

import email.app.EmailCfg;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.BeanScan;
import org.jmouse.mvc.HandlerMapping;
import org.jmouse.mvc.Model;
import org.jmouse.mvc.RoutePath;
import org.jmouse.mvc.mapping.ControllerRegistration;
import org.jmouse.web.context.WebBeanContext;

import java.io.Writer;

@BeanFactories
@BeanScan(EmailCfg.class)
public class WebConfig {

    @Bean
    public ControllerRegistration cfgRegistration() {
        return new ControllerRegistration("/cfg", (request, response)
                -> response.getWriter().write("web_config"));
    }

    @Bean
    public ControllerRegistration helloRegistration(WebBeanContext webBeanContext) {
        return new ControllerRegistration("/hello/{id:int}/{active:boolean}", (request, response) -> {
            RoutePath routePath = (RoutePath) request.getAttribute(HandlerMapping.ROUTE_PATH_ATTRIBUTE);
            Writer    writer    = response.getWriter();

            webBeanContext.getBean(Model.class);

            writer.write("<h1>Hello! ID: %s</h1>".formatted(routePath.variables().get("id")));
        });
    }

}
