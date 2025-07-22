package app.api.config;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.mvc.mapping.DirectRequestPathMapping;

@BeanFactories
public class ApiConfig {

    @Bean
    public DirectRequestPathMapping.Registration appRegistration() {
        return new DirectRequestPathMapping.Registration("/app", (request, response)
                -> response.getWriter().write("app_api"));
    }

    @Bean
    public DirectRequestPathMapping.Registration webRegistration() {
        return new DirectRequestPathMapping.Registration("/web", (request, response)
                -> response.getWriter().write("app_api_web"));
    }

}
