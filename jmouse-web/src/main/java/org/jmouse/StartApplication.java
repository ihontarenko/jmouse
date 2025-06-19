package org.jmouse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.beans.annotation.Factories;
import org.jmouse.beans.annotation.Provide;
import org.jmouse.context.ApplicationBeanContext;
import org.jmouse.mvc.WebApplicationLauncher;
import org.jmouse.mvc.WebMvcInitializer;
import org.jmouse.mvc.mapping.DirectRequestPathMapping.Registration;

import java.io.IOException;

public class StartApplication {

    public static void main(String... arguments) {
        new WebApplicationLauncher(StartApplication.class, ApplicationBeanContext.class).launch();
    }

    @Factories
    public static class AppFactories {

        @Provide
        public Registration helloPage() {
            return new Registration("/hello", (request, response) -> response.getWriter().write("Hello World!"));
        }

        @Provide
        public Registration worldPage() {
            return new Registration("/world", (request, response) -> response.getWriter().write("Hello World!!!"));
        }

        @Provide
        public Registration defaultPage() {
            return new Registration("/page", this::page);
        }

        public void page(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_ACCEPTED);
            response.getWriter().write("<h1>org.jmouse.StartApplication.AppFactories.page</h1>");
        }

    }

    public static class AppInitializer implements WebMvcInitializer<Object> {

        @Override
        public void initialize(Object object) {
            System.out.println("Initializing " + object.getClass().getName());
        }

        @Override
        public Class<Object> objectClass() {
            return Object.class;
        }
    }

}
