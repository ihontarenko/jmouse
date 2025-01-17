package svit.web;

import svit.beans.ScannerBeanContextInitializer;
import svit.observer.EventManager;
import svit.observer.EventManagerFactory;
import svit.web.context.ApplicationContext;
import svit.web.context.ApplicationContextEvent;
import svit.web.context.RootApplicationBeanContext;
import svit.web.context.initializer.WebBeanContextLoaderInitializer;
import svit.web.server.WebServer;
import svit.web.server.WebServerFactory;
import svit.web.server.tomcat.TomcatWebServerConfigurer;

import static svit.web.context.ApplicationContextEvent.EVENT_AFTER_CONTEXT_REFRESH;
import static svit.web.context.ApplicationContextEvent.EVENT_BEFORE_CONTEXT_REFRESH;

public class WebApplicationLauncher {

    private final EventManager eventManager;
    private final Class<?>[]   baseClasses;

    public WebApplicationLauncher(Class<?>[] baseClasses) {
        this.baseClasses = baseClasses;
        this.eventManager = EventManagerFactory.create(baseClasses);
    }

    public static ApplicationContext launch(Class<?>... baseClasses) {
        WebApplicationLauncher launcher = new WebApplicationLauncher(baseClasses);

        ApplicationContext applicationContext = launcher.createApplicationContext();

        applicationContext.addInitializer(new ScannerBeanContextInitializer(baseClasses));

        launcher.refreshContext(applicationContext);

        WebServerFactory factory = applicationContext.getBean(WebServerFactory.class);

        WebServer webServer = factory.getWebServer(new WebBeanContextLoaderInitializer());

        webServer.start();

        return applicationContext;
    }

    public ApplicationContext createApplicationContext() {
        ApplicationContext context = new RootApplicationBeanContext(baseClasses);

        refreshContext(context);

        context.registerBean(EventManager.class, () -> eventManager);

        return context;
    }

    protected void refreshContext(ApplicationContext context) {
        eventManager.notify(new ApplicationContextEvent(EVENT_BEFORE_CONTEXT_REFRESH, context, this));
        context.refresh();
        eventManager.notify(new ApplicationContextEvent(EVENT_AFTER_CONTEXT_REFRESH, context, this));
    }

}
