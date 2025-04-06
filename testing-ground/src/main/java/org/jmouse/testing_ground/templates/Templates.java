package org.jmouse.testing_ground.templates;

import org.jmouse.el.renderable.Engine;
import org.jmouse.el.renderable.LeafsEngine;
import org.jmouse.el.core.rendering.Content;
import org.jmouse.el.core.rendering.Template;
import org.jmouse.el.renderable.loader.ClasspathLoader;
import org.jmouse.el.renderable.loader.TemplateLoader;

import java.util.Map;

public class Templates {

    public static void main(String[] args) {
        TemplateLoader<String> loader = new ClasspathLoader();

        loader.setPrefix("templates/");
        loader.setSuffix(".html");

        Engine engine = new LeafsEngine();

        engine.setLoader(loader);

        Template template = engine.getTemplate("sub_sub");

        Content content = template.render(Map.of("name", "Leafs Template"));

        System.out.println(content.toString());
    }

}
