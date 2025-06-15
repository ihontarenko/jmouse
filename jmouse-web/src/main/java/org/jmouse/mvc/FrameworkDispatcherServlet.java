package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.bind.VirtualProperty;
import org.jmouse.core.i18n.MessageSource;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.renderable.*;
import org.jmouse.el.renderable.loader.ClasspathLoader;
import org.jmouse.el.renderable.loader.TemplateLoader;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.request.http.HttpMethod;
import org.jmouse.web.request.http.HttpStatus;
import org.jmouse.web.servlet.ServletDispatcher;

import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.util.Map;

public class FrameworkDispatcherServlet extends ServletDispatcher {

    public FrameworkDispatcherServlet(WebBeanContext context) {
        super(context);
    }

    @Override
    protected void doDispatch(HttpServletRequest rq, HttpServletResponse rs, HttpMethod method) throws IOException {
        Writer writer = rs.getWriter();

        rs.setStatus(HttpStatus.OK.getCode());

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
