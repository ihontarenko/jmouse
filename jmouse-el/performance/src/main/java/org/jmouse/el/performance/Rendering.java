package org.jmouse.el.performance;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.renderable.*;
import org.jmouse.el.renderable.loader.ClasspathLoader;
import org.jmouse.el.renderable.loader.TemplateLoader;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

import java.util.Map;

public class Rendering extends BaseBenchmark {

    private Map<String, Object> data;
    private Engine              engine;
    private Renderer            renderer;

    @Setup
    public void setup() {
        TemplateLoader<String> loader = new ClasspathLoader();

        loader.setPrefix("templates/");
        loader.setSuffix(".j.html");

        Engine engine = new TemplateEngine();

        engine.setLoader(loader);


        renderer = new TemplateRenderer(engine);

        this.data = this.getContext();
    }

    @Benchmark
    public String benchmark() {
        Template          template = engine.getTemplate("benchmark");
        EvaluationContext context  = template.newContext();

        data.forEach(context::setValue);

        Content content = renderer.render(template, context);

        return content.toString();
    }

}
