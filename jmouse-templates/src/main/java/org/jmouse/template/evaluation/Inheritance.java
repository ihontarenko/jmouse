package org.jmouse.template.evaluation;

import org.jmouse.template.Template;

import java.util.ArrayList;
import java.util.List;

public class Inheritance implements TemplateInheritance {

    private final List<Template> hierarchy = new ArrayList<>();
    private int                  depth     = 0;

    @Override
    public void inherit(Template template) {
        hierarchy.add(template);
    }

    @Override
    public void ascend() {
        if (hierarchy.size() - 1 > depth) {
            depth++;
        }
    }

    @Override
    public void descend() {
        if (depth > 0) {
            depth--;
        }
    }

    @Override
    public Template getChild() {
        Template template = null;

        if (depth > 0) {
            template = hierarchy.get(depth - 1);
        }

        return template;
    }

    @Override
    public Template getParent() {
        Template template = null;

        if (hierarchy.size() - 1 > depth) {
            template = hierarchy.get(depth + 1);
        }

        return template;
    }
}
