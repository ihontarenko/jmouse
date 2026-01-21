package org.jmouse.crawler.dsl.builder;

import org.jmouse.core.Verify;
import org.jmouse.crawler.pipeline.ProcessingPipeline;
import org.jmouse.crawler.route.*;
import org.jmouse.crawler.route.ProcessingRouteResolver;
import org.jmouse.crawler.api.RunContext;
import org.jmouse.crawler.api.ProcessingTask;

import java.util.*;
import java.util.function.Consumer;

public final class RoutesBuilder {

    private final List<ProcessingRoute> routes = new ArrayList<>();

    public RouteSpecification route(String id) {
        return new RouteSpecification(this, id);
    }

    ProcessingRouteResolver build() {
        return new FirstMatchRouteRegistry(routes);
    }

    void add(ProcessingRoute route) {
        routes.add(route);
    }

    public static final class RouteSpecification {

        private final RoutesBuilder parent;
        private final String        id;

        private ProcessingTaskMatcher match = URLMatches.any();
        private ProcessingPipeline    pipeline;

        RouteSpecification(RoutesBuilder parent, String id) {
            this.parent = parent;
            this.id = id;
        }

        public RouteSpecification match(ProcessingTaskMatcher match) {
            this.match = Verify.nonNull(match, "match");
            return this;
        }

        public RouteSpecification pipeline(Consumer<PipelineBuilder> consumer) {
            PipelineBuilder pipelineBuilder = new PipelineBuilder();
            consumer.accept(pipelineBuilder);
            this.pipeline = pipelineBuilder.build();
            return this;
        }

        public void register() {
            parent.add(new SimpleRoute(
                    id, match, Verify.nonNull(pipeline, "pipeline")
            ));
        }

    }

    private record SimpleRoute(
            String id,
            ProcessingTaskMatcher match,
            ProcessingPipeline pipeline
    )
        implements ProcessingRoute {

        private SimpleRoute(
                String id,
                ProcessingTaskMatcher match,
                ProcessingPipeline pipeline
        ) {
            this.id = id;
            this.pipeline = pipeline;
            this.match = match;
        }

        @Override
        public boolean matches(ProcessingTask task, RunContext run) {
            return match.matches(new ProcessingTaskMatcher.Candidate(task.hint(), task, run));
        }

    }
}
