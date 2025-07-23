package email.app;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.mvc.mapping.ControllerRegistration;

@BeanFactories
public class EmailCfg {

    @Bean("email")
    public String getName() {
        return getClass().getName();
    }

    @Bean
    public ControllerRegistration cfgRegistration() {
        return new ControllerRegistration("/email", (request, response)
                -> response.getWriter().write("email_config"));
    }

}
