package org.jmouse.grabber;

import java.time.Duration;
import java.util.List;

/**
 * ⚖️ What a page decided.
 *
 * <p>⚠️ <b>A handler returns one of these rather than the context recording one</b>, which is the
 * whole reason a handler that forgets to decide does not compile. The {@link PageContext} methods
 * that queue work also return the matching verdict, so the decision reads as
 * {@code return page.follow(links);} rather than as two statements that can drift apart.</p>
 *
 * <p>Sealed, and the five cases are the entire control surface of a run.</p>
 */
public sealed interface Verdict {

    /**
     * ➡️ This page led somewhere, and those addresses have been queued.
     */
    record Followed(List<Visit> visits) implements Verdict {

        public Followed {
            visits = visits == null ? List.of() : List.copyOf(visits);
        }

        public boolean isEmpty() {
            return visits.isEmpty();
        }
    }

    /**
     * 🍃 The end of this branch. Nothing more to follow from here.
     *
     * <p>This is the brief's "final page" — the product page rather than the listing, the article
     * rather than the index.</p>
     */
    record Leaf() implements Verdict {
    }

    /**
     * ⏭️ Deliberately not processed. A page outside the scope, one already handled elsewhere, one
     * whose content type nothing here reads.
     */
    record Skipped(String reason) implements Verdict {
    }

    /**
     * ♻️ Try this page again, after this long.
     *
     * <p>The case that exists for the things only a handler can see: a soft block, an interstitial,
     * a page that rendered half. It re-queues the same visit and counts against the same retry
     * budget as a transport failure, so a page cannot loop forever by asking nicely.</p>
     */
    record Retry(Duration after, String reason) implements Verdict {

        public Retry {
            after = after == null ? Duration.ZERO : after;
        }
    }

    /**
     * 🛑 End the whole run. Everything already in flight finishes; nothing new starts.
     */
    record Stopped(String reason) implements Verdict {
    }

    Verdict LEAF = new Leaf();

    static Verdict followed(List<Visit> visits) {
        return new Followed(visits);
    }

    static Verdict leaf() {
        return LEAF;
    }

    static Verdict skipped(String reason) {
        return new Skipped(reason);
    }

    static Verdict retry(Duration after, String reason) {
        return new Retry(after, reason);
    }

    static Verdict stopped(String reason) {
        return new Stopped(reason);
    }

    /**
     * 🛑 Whether this verdict ends the run rather than the branch.
     */
    default boolean endsRun() {
        return this instanceof Stopped;
    }

}
