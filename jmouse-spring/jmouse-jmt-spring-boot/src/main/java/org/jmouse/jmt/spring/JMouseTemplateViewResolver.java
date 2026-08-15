package org.jmouse.jmt.spring;

import org.jmouse.el.template.Engine;
import org.jmouse.el.template.Template;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import java.util.Locale;

public class JMouseTemplateViewResolver implements ViewResolver, Ordered {

    private final Engine engine;
    private String contentType = "text/html;charset=UTF-8";
    private int order = Ordered.LOWEST_PRECEDENCE - 10;

    public JMouseTemplateViewResolver(Engine engine) {
        this.engine = engine;
    }

    @Override
    public View resolveViewName(String viewName, Locale locale) {
        try {
            Template template = engine.getTemplate(viewName);
            JMouseTemplateView view = new JMouseTemplateView(template, engine);
            view.setContentType(contentType);
            return view;
        } catch (Exception exception) {
            return null;
        }
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
