package org.jmouse.crawler.dsl;

import org.jmouse.crawler.routing.*;
import org.jmouse.crawler.runtime.CrawlHint;
import org.jmouse.crawler.routing.CrawlRouteResolver;
import org.jmouse.crawler.runtime.CrawlRunContext;
import org.jmouse.crawler.runtime.CrawlTask;

import java.util.*;
import java.util.function.Consumer;

public final class RoutesBuilder {

    private final List<CrawlRoute> routes = new ArrayList<>();

    public RouteSpec route(String id) {
        return new RouteSpec(this, id);
    }

    CrawlRouteResolver build() {
        return new FirstMatchRouteRegistry(routes);
    }

    void add(CrawlRoute route) {
        routes.add(route);
    }

    public static final class RouteSpec {
        private final RoutesBuilder parent;
        private final String        id;
        private final Set<String>   hintIds = new LinkedHashSet<>();

        private UrlMatch      match = UrlMatches.any();
        private CrawlPipeline pipeline;

        RouteSpec(RoutesBuilder parent, String id) {
            this.parent = parent;
            this.id = id;
        }

        public RouteSpec hints(Object... clientHints) {
            for (Object h : clientHints) {
                if (h == null) continue;
                if (h instanceof CrawlHint ch) hintIds.add(ch.id());
                else if (h instanceof Enum<?> e) hintIds.add(e.name());
                else hintIds.add(String.valueOf(h));
            }
            return this;
        }

        public RouteSpec match(UrlMatch match) {
            this.match = Objects.requireNonNull(match, "match");
            return this;
        }

        public RouteSpec pipeline(Consumer<PipelineBuilder> c) {
            PipelineBuilder pb = new PipelineBuilder();
            c.accept(pb);
            this.pipeline = pb.build();
            return this;
        }

        public void register() {
            if (pipeline == null) {
                throw new IllegalStateException("pipeline is required for route: " + id);
            }
            parent.add(new HintAwareRoute(id, hintIds, match, pipeline));
        }
    }

    private static final class HintAwareRoute implements CrawlRoute {

        private final String        id;
        private final Set<String>   hintIds;
        private final UrlMatch      match;
        private final CrawlPipeline pipeline;

        HintAwareRoute(String id, Set<String> hintIds, UrlMatch match, CrawlPipeline pipeline) {
            this.id = id;
            this.hintIds = Set.copyOf(hintIds);
            this.match = match;
            this.pipeline = pipeline;
        }

        @Override public String id() { return id; }
        @Override public CrawlPipeline pipeline() { return pipeline; }

        @Override
        public boolean matches(CrawlTask task, CrawlRunContext run) {
            return false;
        }

        @Override
        public boolean supportsHint(CrawlHint hint) {
            return hint != null && hintIds.contains(hint.id());
        }
    }
}
