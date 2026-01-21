package org.jmouse.crawler.api;

public sealed interface TaskOrigin
        permits TaskOrigin.Seed, TaskOrigin.Discovered, TaskOrigin.Retry, TaskOrigin.Restored {

    String kind();

    record Seed(String publisher) implements TaskOrigin {
        @Override public String kind() { return "seed"; }
    }

    record Discovered(
            String publisher,
            String routeId,
            TaskId parentId
    ) implements TaskOrigin {
        @Override
        public String kind() {
            return "discovered";
        }
    }

    record Retry(String reason) implements TaskOrigin {
        @Override
        public String kind() {
            return "retry";
        }
    }

    record Restored(String source) implements TaskOrigin {
        @Override
        public String kind() {
            return "restored";
        }
    }

    static TaskOrigin seed(String publisher) {
        return new Seed(publisher);
    }

    static TaskOrigin discovered(String publisher, String routeId, TaskId parentId) {
        return new Discovered(publisher, routeId, parentId);
    }

    static TaskOrigin retry(String reason) {
        return new Retry(reason);
    }

}

