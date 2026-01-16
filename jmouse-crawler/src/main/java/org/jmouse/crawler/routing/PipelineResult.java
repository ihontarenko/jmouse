package org.jmouse.crawler.routing;

import org.jmouse.core.Verify;

public sealed interface PipelineResult
        permits PipelineResult.Goon, PipelineResult.Stop, PipelineResult.Route {

    String stageId();

    default Kind kind() {
        return switch (this) {
            case Goon ignored -> Kind.GOON;
            case Stop ignored -> Kind.STOP;
            case Route ignored -> Kind.ROUTE;
        };
    }

    enum Kind { GOON, STOP, ROUTE }

    record Goon(String stageId) implements PipelineResult {}
    record Stop(String stageId) implements PipelineResult {}
    record Route(String stageId, String routeId) implements PipelineResult {}

    static PipelineResult goon(String stageId) { return new Goon(stageId); }
    static PipelineResult stop(String stageId) { return new Stop(stageId); }
    static PipelineResult route(String stageId, String routeId) { return new Route(stageId, routeId); }
}

