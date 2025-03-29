package org.jmouse.testing_ground.templates;

import org.jmouse.el.rendering.Content;
import org.jmouse.el.rendering.Template;
import org.jmouse.template.*;
import org.jmouse.template.loader.ClasspathLoader;
import org.jmouse.template.loader.TemplateLoader;

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
