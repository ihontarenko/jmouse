package _app;

import org.jmouse.context.ApplicationBeanContext;
import org.jmouse.jMouseWebRoot;
import org.jmouse.mvc.WebApplicationLauncher;
import org.jmouse.mvc.BeanInstanceInitializer;
import org.jmouse.mvc.mapping.DirectRequestPathMapping;

public class StartApplication {

    public static void main(String... arguments) {
        // todo: resolve scopes of contexts
        new WebApplicationLauncher(StartApplication.class, ApplicationBeanContext.class, jMouseWebRoot.class).launch();
    }

    public static class AppInitializer implements BeanInstanceInitializer<DirectRequestPathMapping> {

        @Override
        public void initialize(DirectRequestPathMapping object) {
            object.addController("/index", (request, response)
                    -> response.getWriter().write("Hello Index!"));
        }

        @Override
        public Class<DirectRequestPathMapping> objectClass() {
            return DirectRequestPathMapping.class;
        }
    }

}
