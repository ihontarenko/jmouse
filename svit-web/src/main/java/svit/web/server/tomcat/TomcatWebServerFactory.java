package svit.web.server.tomcat;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.apache.catalina.startup.Tomcat;
import svit.beans.BeanContext;
import svit.beans.BeanContextAware;
import svit.web.ApplicationInitializer;
import svit.web.context.WebBeanContext;
import svit.web.server.WebServer;
import svit.web.server.WebServerFactory;

import java.util.Set;

public class TomcatWebServerFactory implements WebServerFactory, BeanContextAware {

    private BeanContext context;

    /**
     * Retrieves the {@link BeanContext} associated with this component.
     *
     * @return the current {@link BeanContext}.
     */
    @Override
    public BeanContext getBeanContext() {
        return context;
    }

    /**
     * Sets the {@link BeanContext} for this component.
     *
     * @param context the {@link BeanContext} to set.
     */
    @Override
    public void setBeanContext(BeanContext context) {
        this.context = context;
    }

    @Override
    public WebServer getWebServer(ApplicationInitializer... initializers) {
        WebServer                    webServer  = new TomcatWebServer();
        WebServer.Configurer<Tomcat> configurer = new TomcatWebServerConfigurer(8899, new Starter());

        if (webServer.server() instanceof Tomcat tomcat) {
            configurer.configure(tomcat);
        }

        return webServer;
    }

    class Starter implements ServletContainerInitializer {

        @Override
        public void onStartup(Set<Class<?>> classes, ServletContext ctx) throws ServletException {
            if (context instanceof WebBeanContext webBeanContext) {
                webBeanContext.setServletContext(ctx);
            }

        }

    }

}
