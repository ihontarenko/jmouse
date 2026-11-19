package org.jmouse.vm.graph;

public final class ReachabilityLimitException extends IllegalStateException {

    private final ReachabilityLimit limit;
    private final long maximum;

    public ReachabilityLimitException(ReachabilityLimit limit, long maximum) {
        super("Reachability limit exceeded: " + limit + " (" + maximum + ")");
        this.limit = limit;
        this.maximum = maximum;
    }

    public ReachabilityLimit limit() {
        return limit;
    }

    public long maximum() {
        return maximum;
    }
}
