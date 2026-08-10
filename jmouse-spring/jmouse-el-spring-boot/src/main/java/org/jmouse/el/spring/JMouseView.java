package org.jmouse.el.spring;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.template.Content;
import org.jmouse.el.template.Engine;
import org.jmouse.el.template.Renderer;
import org.jmouse.el.template.Template;
import org.jmouse.el.template.TemplateRenderer;
import org.springframework.web.servlet.View;

import java.util.Map;

public class JMouseView implements View {

    private final Template template;
    private final Engine engine;
    private String contentType = "text/html;charset=UTF-8";

    public JMouseView(Template template, Engine engine) {
        this.template = template;
        this.engine = engine;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        EvaluationContext context = template.newContext();

        if (model != null) {
            model.forEach(context::setValue);
        }

        context.setValue("request", request);
        context.setValue("session", request.getSession(false));

        Renderer renderer = new TemplateRenderer(engine);
        Content content = renderer.render(template, context);

        response.setContentType(contentType);
        response.getWriter().write(content.getDataArray());
    }
}
