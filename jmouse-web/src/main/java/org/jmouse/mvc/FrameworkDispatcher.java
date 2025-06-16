package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.bind.VirtualProperty;
import org.jmouse.core.i18n.MessageSource;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.renderable.*;
import org.jmouse.el.renderable.loader.ClasspathLoader;
import org.jmouse.el.renderable.loader.TemplateLoader;
import org.jmouse.mvc.handler.Controller;
import org.jmouse.mvc.mapping.ControllerMapping;
import org.jmouse.mvc.mapping.DirectMapping;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.request.http.HttpMethod;
import org.jmouse.web.request.http.HttpStatus;
import org.jmouse.web.servlet.ServletDispatcher;

import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FrameworkDispatcher extends ServletDispatcher {

    private List<HandlerMapping> mappings = new ArrayList<>();

    public static final String DEFAULT_DISPATCHER = "defaultDispatcher";

    public FrameworkDispatcher(WebBeanContext context) {
        super(context);

        DirectMapping mapping = new DirectMapping();

        mappings.add(new ControllerMapping());
        mappings.add(mapping);

        mapping.addController("/index", (request, response) -> {
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().print("index");
        });
    }

    @Override
    protected void doDispatch(HttpServletRequest request, HttpServletResponse response, HttpMethod method)
            throws IOException {

        response.setStatus(HttpStatus.OK.getCode());
        boolean skip = false;

        for (HandlerMapping mapping : mappings) {
            Object handler = mapping.getHandler(request);

            if (handler != null) {
                if (handler instanceof Controller controller) {
                    controller.handle(request, response);
                    skip = true;
                }
            }
        }

        Writer writer = response.getWriter();

        if (skip) {
            return;
        }

        TemplateLoader<String> loader = new ClasspathLoader();

        loader.setPrefix("templates/");
        loader.setSuffix(".j.html");

        TemplateEngine engine = new TemplateEngine();

        engine.setLoader(loader);

        Template template = engine.getTemplate("jmouse");
        EvaluationContext evaluationContext = template.newContext();

        evaluationContext.setValue("title", context.getBean(MessageSource.class).getMessage("project.name"));
        evaluationContext.setValue("servlet", getServletName());
        evaluationContext.setValue("uname", "ZAiS");
        evaluationContext.setValue("randomString_requestScoped", context.getBean("s1"));
        evaluationContext.setValue("randomString_sessionScoped", context.getBean("s2"));
        evaluationContext.setValue("timestamp", Instant.now().toString());
        evaluationContext.setValue("instant", Instant.now());
        evaluationContext.setValue("dt", Map.of("now", Instant.now()));

        evaluationContext.getVirtualProperties()
                .addVirtualProperty(new VirtualProperty<Instant>() {
                    @Override
                    public Class<Instant> getType() {
                        return Instant.class;
                    }

                    @Override
                    public String getName() {
                        return "nanoSeconds";
                    }

                    @Override
                    public Object readValue(Instant object) {
                        return object.getNano() + "ns";
                    }

                    @Override
                    public boolean isReadable() {
                        return true;
                    }
                });

        Renderer renderer = new TemplateRenderer(engine);
        Content  content  = renderer.render(template, evaluationContext);

        writer.write(content.getDataArray());


    }

}
