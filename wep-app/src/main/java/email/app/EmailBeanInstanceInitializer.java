package email.app;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.web.mvc.BeanConfigurer;

@Bean
public class EmailBeanInstanceInitializer implements BeanConfigurer<EmailCfg> {
    /**
     * 🔧 Apply additional configuration to the target object.
     *
     * @param object instance to initialize
     */
    @Override
    public void configure(EmailCfg object) {
        System.out.println(object);
    }

}
