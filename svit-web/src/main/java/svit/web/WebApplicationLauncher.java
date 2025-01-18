package svit.web;

import svit.beans.ScannerBeanContextInitializer;
import svit.observer.EventManager;
import svit.observer.EventManagerFactory;
import svit.web.context.ApplicationBeanContext;
import svit.web.context.ApplicationContextEvent;
import svit.web.context.WebApplicationBeanContext;
import svit.web.context.initializer.WebBeanContextServletInitializer;
import svit.web.server.WebServer;
import svit.web.server.WebServerFactory;

import static svit.web.context.ApplicationContextEvent.EVENT_AFTER_CONTEXT_REFRESH;
import static svit.web.context.ApplicationContextEvent.EVENT_BEFORE_CONTEXT_REFRESH;

public class WebApplicationLauncher {

    private final EventManager eventManager;
    private final Class<?>[]   baseClasses;

    public WebApplicationLauncher(Class<?>[] baseClasses) {
        this.baseClasses = baseClasses;
        this.eventManager = EventManagerFactory.create(baseClasses);
    }

    public static ApplicationBeanContext launch(Class<?>... baseClasses) {
        WebApplicationLauncher launcher = new WebApplicationLauncher(baseClasses);

        ApplicationBeanContext applicationContext = launcher.createApplicationContext();
        applicationContext.addInitializer(new ScannerBeanContextInitializer(baseClasses));
        launcher.refreshContext(applicationContext);

        WebServerFactory factory = applicationContext.getBean(WebServerFactory.class);
        WebServer webServer = factory.getWebServer(new WebBeanContextServletInitializer());
        webServer.start();

        return applicationContext;
    }

    public ApplicationBeanContext createApplicationContext() {
        ApplicationBeanContext context = new WebApplicationBeanContext(baseClasses);
        refreshContext(context);
        context.registerBean(EventManager.class, () -> eventManager);
        return context;
    }

    protected void refreshContext(ApplicationBeanContext context) {
        eventManager.notify(new ApplicationContextEvent(EVENT_BEFORE_CONTEXT_REFRESH, context, this));
        context.refresh();
        eventManager.notify(new ApplicationContextEvent(EVENT_AFTER_CONTEXT_REFRESH, context, this));
    }

}
