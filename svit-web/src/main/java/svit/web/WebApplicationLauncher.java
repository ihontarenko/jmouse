package svit.web;

import svit.beans.BeanScope;
import svit.beans.ScannerBeanContextInitializer;
import svit.beans.Scope;
import svit.convert.Conversion;
import svit.observer.EventManager;
import svit.observer.EventManagerFactory;
import svit.reflection.ClassFinder;
import svit.reflection.JavaType;
import svit.util.Sorter;
import svit.web.context.ApplicationBeanContext;
import svit.web.context.ApplicationContextEvent;
import svit.web.context.WebApplicationBeanContext;
import svit.web.context.initializer.WebBeanContextServletInitializer;
import svit.web.server.WebServer;
import svit.web.server.WebServerFactory;

import java.util.ArrayList;
import java.util.List;

import static svit.web.context.ApplicationContextEvent.EVENT_AFTER_CONTEXT_REFRESH;
import static svit.web.context.ApplicationContextEvent.EVENT_BEFORE_CONTEXT_REFRESH;

public class WebApplicationLauncher {

    private final EventManager eventManager;
    private final Class<?>[]   baseClasses;

    public WebApplicationLauncher(Class<?>[] baseClasses) {
        this.baseClasses = baseClasses;
        this.eventManager = EventManagerFactory.create(baseClasses);
    }

    public static void launch(Class<?>... baseClasses) {
        WebApplicationLauncher launcher = new WebApplicationLauncher(baseClasses);

        ScannerBeanContextInitializer scannerBeanContextInitializer = new ScannerBeanContextInitializer(baseClasses);

        scannerBeanContextInitializer.addScanner(
                rootTypes -> new ArrayList<>(ClassFinder.findImplementations(ApplicationInitializer.class, rootTypes)));

        ApplicationBeanContext applicationContext = launcher.createApplicationContext();
        JavaType.forClass(ScannerBeanContextInitializer.class).toHierarchyString(0);
        applicationContext.addInitializer(scannerBeanContextInitializer);
        launcher.refreshContext(applicationContext);

        List<ApplicationInitializer> initializers = applicationContext.getBeans(ApplicationInitializer.class);
        Sorter.sort(initializers);

        for (ApplicationInitializer initializer : initializers) {
            System.out.println(JavaType.forInstance(initializer).toHierarchyString(0));
        }
        
        BeanScope beanScope = applicationContext.getBean(Conversion.class)
                .convert(3, BeanScope.class);

        System.out.println(
                JavaType.forInstance(beanScope).locate(Scope.class).getRawType()
        );

        System.out.println("beanScope by number: " + beanScope);
        System.out.println("JavaType cache size: " + JavaType.getCacheSize());

        WebServerFactory factory = applicationContext.getBean(WebServerFactory.class);
        WebServer webServer = factory.getWebServer(new WebBeanContextServletInitializer());
        webServer.start();

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
