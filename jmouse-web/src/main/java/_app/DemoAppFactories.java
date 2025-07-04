package _app;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.context.BeanConditionIfProperty;
import org.jmouse.mvc.mapping.DirectRequestPathMapping;

import java.io.IOException;

@BeanFactories
@BeanConditionIfProperty(name = "app.name", value = "jMouse")
public class DemoAppFactories {

    @Bean
    public DirectRequestPathMapping.Registration helloPage() {
        return new DirectRequestPathMapping.Registration("/hello", (request, response) -> response.getWriter().write("Hello World!"));
    }

    @Bean
    public DirectRequestPathMapping.Registration worldPage() {
        return new DirectRequestPathMapping.Registration("/world", (request, response) -> response.getWriter().write("Hello World!!!"));
    }

    @Bean
    public DirectRequestPathMapping.Registration defaultPage() {
        return new DirectRequestPathMapping.Registration("/page", this::page);
    }

    public void page(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_ACCEPTED);
        response.getWriter().write("<h1>_app.StartApplication.AppFactories.page</h1>");
    }

}
