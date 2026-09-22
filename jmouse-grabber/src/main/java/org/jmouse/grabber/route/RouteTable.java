package org.jmouse.grabber.route;

import java.util.List;
import java.util.Optional;

import org.jmouse.grabber.Visit;

/**
 * 🗺️ The routes, in the order they were declared.
 *
 * <p>First match wins, which is the rule a reader can hold in their head — and it means a general
 * route written above a specific one silently swallows the specific one. That is a real trap and the
 * only defence is order, so the order is the caller's and is never re-sorted here.</p>
 *
 * <p>⚠️ A visit carrying a <b>route hint</b> — set by the page that discovered it — goes straight to
 * that route without matching. A hint naming a route that does not exist falls through to matching
 * rather than failing: a run that dies because one page named a route that was renamed is worse than
 * one that handles the page with whatever claims it.</p>
 */
public record RouteTable(List<GrabRoute> routes) {

    public RouteTable {
        routes = routes == null ? List.of() : List.copyOf(routes);
    }

    public static RouteTable of(List<GrabRoute> routes) {
        return new RouteTable(routes);
    }

    /**
     * 🔎 The route that handles this visit, if any.
     */
    public Optional<GrabRoute> resolve(Visit visit) {
        if (visit.route() != null) {
            Optional<GrabRoute> hinted = named(visit.route());

            if (hinted.isPresent()) {
                return hinted;
            }
        }

        for (GrabRoute route : routes) {
            if (route.claims(visit)) {
                return Optional.of(route);
            }
        }

        return Optional.empty();
    }

    public Optional<GrabRoute> named(String name) {
        return routes.stream().filter(route -> route.name().equals(name)).findFirst();
    }

    public boolean isEmpty() {
        return routes.isEmpty();
    }

    public int size() {
        return routes.size();
    }

}
