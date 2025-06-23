package _app;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.beans.annotation.Factories;
import org.jmouse.beans.annotation.Provide;
import org.jmouse.mvc.mapping.DirectRequestPathMapping;

import java.io.IOException;

@Factories
public class DemoAppFactories {

    @Provide
    public DirectRequestPathMapping.Registration helloPage() {
        return new DirectRequestPathMapping.Registration("/hello", (request, response) -> response.getWriter().write("Hello World!"));
    }

    @Provide
    public DirectRequestPathMapping.Registration worldPage() {
        return new DirectRequestPathMapping.Registration("/world", (request, response) -> response.getWriter().write("Hello World!!!"));
    }

    @Provide
    public DirectRequestPathMapping.Registration defaultPage() {
        return new DirectRequestPathMapping.Registration("/page", this::page);
    }

    public void page(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_ACCEPTED);
        response.getWriter().write("<h1>_app.StartApplication.AppFactories.page</h1>");
    }

}
