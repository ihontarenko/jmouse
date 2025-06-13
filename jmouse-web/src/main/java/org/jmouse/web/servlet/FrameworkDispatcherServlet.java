package org.jmouse.web.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.MediaType;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.renderable.*;
import org.jmouse.el.renderable.loader.ClasspathLoader;
import org.jmouse.el.renderable.loader.TemplateLoader;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.http.HttpStatus;

import java.io.IOException;
import java.io.Writer;

public class FrameworkDispatcherServlet extends FrameworkDispatcher {

    public FrameworkDispatcherServlet(WebBeanContext context) {
        super(context);
    }

    @Override
    protected void doDispatch(HttpServletRequest rq, HttpServletResponse rs, HttpMethod method) throws IOException {
        Writer writer = rs.getWriter();

        rs.setStatus(HttpStatus.OK.getCode());

        writer.write("<h1>ZAiS</h1>");
        writer.write("<h1>"+ rs.encodeURL("/") +"</h1>");
        writer.write(method.name());

        writer.write("<h2>");
        writer.write(rq.getHeader("User-Agent"));
        writer.write("</h2>");

        writer.write("<h3>");
        writer.write(getServletName());
        writer.write("</h3>");

        writer.write("<h4>1. ");
        writer.write((String) context.getBean("s1"));
        writer.write("</h4>");

        writer.write("<h4>2. ");
        writer.write((String) context.getBean("s1"));
        writer.write("</h4>");

        writer.write("<h4>3. ");
        writer.write((String) context.getBean("s2"));
        writer.write("</h4>");

        writer.write("<h4>EL ");
        writer.write(context.getBean(ExpressionLanguage.class).evaluate("22 | bigDecimal / 7", String.class));
        writer.write("</h4>");

        writer.write("<h2>" + getServletName() + "</h2>");

        TemplateLoader<String> loader = new ClasspathLoader();

        loader.setPrefix("templates/");
        loader.setSuffix(".j.html");

        TemplateEngine engine = new TemplateEngine();

        engine.setLoader(loader);

        Template template = engine.getTemplate("jmouse");
        EvaluationContext evaluationContext = template.newContext();

        evaluationContext.setValue("title", getServletName());

        Renderer renderer = new TemplateRenderer(engine);
        Content  content  = renderer.render(template, evaluationContext);

        writer.write(content.getDataArray());


    }

}
