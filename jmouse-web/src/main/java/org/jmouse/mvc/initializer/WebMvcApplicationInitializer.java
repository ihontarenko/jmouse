package org.jmouse.mvc.initializer;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.jmouse.beans.annotation.BeanConstructor;
import org.jmouse.core.reflection.JavaType;
import org.jmouse.mvc.WebMvcInitializer;
import org.jmouse.mvc.mapping.DirectRequestPathMapping;
import org.jmouse.mvc.mapping.DirectRequestPathMapping.Registration;
import org.jmouse.util.Priority;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.initializer.WebApplicationInitializer;

import java.lang.reflect.Array;
import java.util.List;

@Priority(1)
public class WebMvcApplicationInitializer implements WebApplicationInitializer {

    private final WebBeanContext context;

    @BeanConstructor
    public WebMvcApplicationInitializer(WebBeanContext context) {
        this.context = context;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void onStartup(ServletContext servletContext) throws ServletException {
        DirectRequestPathMapping requestPathMapping = context.getBean(DirectRequestPathMapping.class);
        List<Registration>       registrations      = context.getBeans(Registration.class);


        for (WebMvcInitializer bean : context.getBeans(WebMvcInitializer.class)) {

            if (bean.objectClass().isInstance(requestPathMapping)) {
                System.out.println("Registering " + bean.objectClass().getName());
            }

            bean.initialize(requestPathMapping);

            System.out.println(bean.objectClass());
        }

        for (Registration registration : registrations) {
            requestPathMapping.addController(registration.getRoute(), registration.getController());
        }

        System.out.println(registrations);
    }

}
