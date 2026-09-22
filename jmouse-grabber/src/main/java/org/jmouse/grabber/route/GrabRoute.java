package org.jmouse.grabber.route;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.matcher.TextMatchers;
import org.jmouse.grabber.GrabPolicy;
import org.jmouse.grabber.PageContext;
import org.jmouse.grabber.PageHandler;
import org.jmouse.grabber.Visit;
import org.jmouse.grabber.VisitKey;

/**
 * 🛣️ One kind of page, and what to do with it.
 *
 * <p>⚠️ The condition is an {@link org.jmouse.core.matcher.Matcher}, and the grabber deliberately
 * defines no predicate language of its own: {@code TextMatchers.ant("/product/**")},
 * {@code contains}, {@code startsWith} and their {@code and}/{@code or}/{@code not} combinators
 * already say everything a route needs, and a caller can pass a lambda for anything they do not.</p>
 *
 * @param name       what this route is called — it appears in logs, in a failure and in a route hint
 * @param condition  which visits it claims
 * @param handler    what to do with one
 * @param narrowing  a policy narrowing for this route only, or {@code null}
 * @param keyOf      how a page handled by this route is identified, or {@code null} for the address
 */
public record GrabRoute(
        String name,
        Matcher<Visit> condition,
        PageHandler handler,
        GrabPolicy narrowing,
        Function<PageContext, VisitKey> keyOf
) {

    public GrabRoute {
        Objects.requireNonNull(name, "A route needs a name");
        Objects.requireNonNull(condition, "A route needs a condition");
        Objects.requireNonNull(handler, "A route needs a handler");
    }

    /**
     * 🛣️ A route claiming the visits this matcher accepts.
     */
    public static GrabRoute of(String name, Matcher<Visit> condition, PageHandler handler) {
        return new GrabRoute(name, condition, handler, null, null);
    }

    /**
     * 🛣️ A route claiming the visits whose ADDRESS this matcher accepts — the ordinary case, and the
     * one that lets {@code TextMatchers} be used directly without a lambda in between.
     */
    public static GrabRoute onAddress(String name, Matcher<String> address, PageHandler handler) {
        return of(name, address(address), handler);
    }

    /**
     * 🛣️ A route claiming everything nothing else claimed.
     */
    public static GrabRoute anything(String name, PageHandler handler) {
        return of(name, Matcher.constant(true), handler);
    }

    /**
     * 🔎 Lifts a matcher on the address text into one on the visit.
     */
    public static Matcher<Visit> address(Matcher<String> matcher) {
        return visit -> matcher.matches(visit.address().toString());
    }

    /**
     * 🔎 A matcher on the address path alone, which is what a route usually means when it says
     * {@code /product/}.
     */
    public static Matcher<Visit> path(Matcher<String> matcher) {
        return visit -> {
            String path = visit.address().getPath();
            return matcher.matches(path == null ? "/" : path);
        };
    }

    /**
     * 🔎 A matcher on the host — for a run spanning several sites that handles each differently.
     */
    public static Matcher<Visit> host(String hostName) {
        Matcher<String> same = TextMatchers.same(hostName);
        return visit -> {
            URI address = visit.address();
            return address.getHost() != null && same.matches(address.getHost());
        };
    }

    /**
     * 🔒 The same route, with its own policy narrowing.
     */
    public GrabRoute narrowedBy(GrabPolicy policy) {
        return new GrabRoute(name, condition, handler, policy, keyOf);
    }

    /**
     * 🔑 The same route, identifying its pages by something other than the address.
     *
     * <p>The mechanism behind "one product reachable at two addresses is one product".</p>
     */
    public GrabRoute identifiedBy(Function<PageContext, VisitKey> key) {
        return new GrabRoute(name, condition, handler, narrowing, key);
    }

    public boolean claims(Visit visit) {
        return condition.matches(visit);
    }

    /**
     * ⚙️ This route's effective policy, given the run's.
     */
    public GrabPolicy policyWithin(GrabPolicy runPolicy) {
        return runPolicy.narrowedBy(narrowing);
    }

    public Optional<Function<PageContext, VisitKey>> identity() {
        return Optional.ofNullable(keyOf);
    }

    @Override
    public String toString() {
        return "GrabRoute[" + name + " " + condition + "]";
    }

}
