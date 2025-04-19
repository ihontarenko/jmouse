package org.jmouse.el.performance;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.performance.model.Stock;
import org.jmouse.el.renderable.*;
import org.jmouse.el.renderable.loader.ClasspathLoader;
import org.jmouse.el.renderable.loader.TemplateLoader;
import org.openjdk.jmh.annotations.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Fork(5)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 2, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class Rendering {

    private Map<String, Object> data;
    private Engine              engine;
    private Renderer            renderer;
    private Template            template;
    private EvaluationContext   context;

    @Setup
    public void setup() {
        TemplateLoader<String> loader = new ClasspathLoader();

        loader.setPrefix("templates/");
        loader.setSuffix(".j.html");

        engine = new TemplateEngine();
        engine.setLoader(loader);

        renderer = new TemplateRenderer(engine);

        template = engine.getTemplate("simple");
        context = template.newContext();

        data = getContext();

        data.forEach(context::setValue);

        this.data = this.getContext();
    }

    @Benchmark
    public String resolveProperties() {

        for (int i = 0; i < 20; i++) {
            context.getValue("items[0].name");
            context.getValue("items[0].name2");
            context.getValue("items[0].symbol");
            context.getValue("items[0].url");
            context.getValue("items[0].ratio");

            context.getValue("items[1].name");
            context.getValue("items[1].name2");
            context.getValue("items[1].symbol");
            context.getValue("items[1].url");
            context.getValue("items[1].ratio");

            context.getValue("items[2].name");
            context.getValue("items[2].name2");
            context.getValue("items[2].symbol");
            context.getValue("items[2].url");
            context.getValue("items[2].ratio");
            context.getValue("items[2].ratio");

            context.getValue("items[3].name");
            context.getValue("items[3].name2");
            context.getValue("items[3].symbol");
            context.getValue("items[3].url");
            context.getValue("items[3].ratio");
        }

        return "";
    }

//    @Benchmark
//    public String benchmark() {
//        Content content = renderer.render(template, context);
//
//        return content.toString();
//    }

    private Map<String, Object> getContext() {
        Map<String, Object> context = new HashMap<>();
        System.out.println("data: " + Stock.dummyItems().size());
        context.put("items", Stock.dummyItems());
        return context;
    }

}
