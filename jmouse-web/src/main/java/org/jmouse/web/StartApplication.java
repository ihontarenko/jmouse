package org.jmouse.web;

import org.jmouse.context.ApplicationBeanContext;
import org.jmouse.core.bind.Bind;
import org.jmouse.core.convert.Conversion;
import org.jmouse.web.context.WebBeanContext;

import java.time.LocalDateTime;

public class StartApplication {

    public static void main(String... arguments) {
        WebBeanContext context = new WebApplicationLauncher(StartApplication.class, ApplicationBeanContext.class).launch();

        System.out.println("--- Java Home ---");
        Bind.with(context.getEnvironment()).toString("JAVA_HOME").ifPresent(System.out::println);

        Conversion conversion = context.getBean(Conversion.class);

        System.out.println(
                conversion.convert(213123123D, LocalDateTime.class)
        );

        System.out.println(context.getProperty("jmouse.name"));
    }

}
