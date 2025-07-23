package app.api;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.mvc.mapping.ControllerRegistration;
import org.jmouse.mvc.mapping.DirectRequestPathMapping;

@BeanFactories
public class WebConfig {

    @Bean
    public ControllerRegistration cfgRegistration() {
        return new ControllerRegistration("/cfg", (request, response)
                -> response.getWriter().write("web_config"));
    }

}
