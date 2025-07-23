package app.api;

import email.app.EmailCfg;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.BeanScan;
import org.jmouse.mvc.mapping.ControllerRegistration;
import org.jmouse.mvc.mapping.DirectRequestPathMapping;

@BeanFactories
@BeanScan(EmailCfg.class)
public class WebConfig {

    @Bean
    public ControllerRegistration cfgRegistration() {
        return new ControllerRegistration("/cfg", (request, response)
                -> response.getWriter().write("web_config"));
    }

}
