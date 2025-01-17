package svit.web;

import jakarta.servlet.ServletContext;
import svit.web.context.ApplicationContext;

public class StartApplication {

    public static void main(String... arguments) {
        ApplicationContext context = WebApplicationLauncher.launch(StartApplication.class);

        ServletContext servletContext = context.getBean(ServletContext.class);

        System.out.println(servletContext);
    }

}
