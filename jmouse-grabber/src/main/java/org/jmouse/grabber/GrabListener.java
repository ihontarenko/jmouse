package org.jmouse.grabber;

import java.util.ArrayList;
import java.util.List;

import org.jmouse.grabber.fetch.PageResponse;

/**
 * 👂 What happened during a run.
 *
 * <p>Every method is a no-op by default, so a listener implements the two it cares about. ⚠️ <b>This
 * is the metrics seam, and the module ships no metrics dependency</b> — a Micrometer binding is
 * thirty lines in the application that wants one, and a library that dragged one in would impose that
 * choice on every application that does not.</p>
 *
 * <p>A listener throwing does not fail the run: it is an observer, and an observer that can stop the
 * work is not an observer.</p>
 */
public interface GrabListener {

    /**
     * 👂 A listener that does nothing.
     */
    static GrabListener none() {
        return new GrabListener() {
        };
    }

    /**
     * 👂 Several listeners as one, each called in turn, each one's failure swallowed so it cannot
     * take the others down with it.
     */
    static GrabListener composite(List<GrabListener> listeners) {
        List<GrabListener> all = List.copyOf(listeners);

        return new GrabListener() {

            @Override
            public void queued(Visit visit) {
                all.forEach(listener -> safely(() -> listener.queued(visit)));
            }

            @Override
            public void requesting(Visit visit) {
                all.forEach(listener -> safely(() -> listener.requesting(visit)));
            }

            @Override
            public void fetched(Visit visit, PageResponse response) {
                all.forEach(listener -> safely(() -> listener.fetched(visit, response)));
            }

            @Override
            public void decided(Visit visit, String route, Verdict verdict) {
                all.forEach(listener -> safely(() -> listener.decided(visit, route, verdict)));
            }

            @Override
            public void emitted(Visit visit, Object item) {
                all.forEach(listener -> safely(() -> listener.emitted(visit, item)));
            }

            @Override
            public void failed(Failure failure) {
                all.forEach(listener -> safely(() -> listener.failed(failure)));
            }

            @Override
            public void finished(GrabRun run) {
                all.forEach(listener -> safely(() -> listener.finished(run)));
            }

            private void safely(Runnable action) {
                try {
                    action.run();
                } catch (RuntimeException ignored) {
                    // An observer that can stop the work is not an observer.
                }
            }
        };
    }

    /**
     * 🪵 A listener that writes a line per event to this module's logger, at debug.
     */
    static GrabListener logging() {
        return new LoggingGrabListener();
    }

    /**
     * 📥 A visit was accepted into the queue — after deduplication and after the caps, so this is
     * work that will actually be done.
     */
    default void queued(Visit visit) {
    }

    /**
     * 🚦 A request is about to go out — the moment the pacing gate released it.
     *
     * <p>⚠️ This, not {@link #fetched}, is the event that measures pacing. The interval between two
     * {@code fetched} events is the interval between two <b>replies</b>, which includes however long
     * each request took: a slow first page and a fast second one make a correctly paced run look like
     * it broke its own delay.</p>
     */
    default void requesting(Visit visit) {
    }

    /**
     * 📬 A page came back, whatever its status.
     */
    default void fetched(Visit visit, PageResponse response) {
    }

    /**
     * ⚖️ A handler decided. The event a debug session actually wants: it says which route claimed the
     * page and what it concluded.
     */
    default void decided(Visit visit, String route, Verdict verdict) {
    }

    /**
     * 📤 Something was emitted to the sink.
     */
    default void emitted(Visit visit, Object item) {
    }

    /**
     * 💥 A visit failed. Called on every attempt, with {@link Failure#exhausted()} saying whether it
     * was the last one.
     */
    default void failed(Failure failure) {
    }

    /**
     * 🏁 The run ended, by any means — drained, capped, or stopped.
     */
    default void finished(GrabRun run) {
    }

    /**
     * 📚 A listener that keeps everything it saw, for a smoke or a test.
     */
    final class Recording implements GrabListener {

        private final List<Visit>   fetched  = new ArrayList<>();
        private final List<Object>  items    = new ArrayList<>();
        private final List<Failure> failures = new ArrayList<>();

        @Override
        public synchronized void fetched(Visit visit, PageResponse response) {
            fetched.add(visit);
        }

        @Override
        public synchronized void emitted(Visit visit, Object item) {
            items.add(item);
        }

        @Override
        public synchronized void failed(Failure failure) {
            failures.add(failure);
        }

        public synchronized List<Visit> fetchedVisits() {
            return List.copyOf(fetched);
        }

        public synchronized List<Object> emittedItems() {
            return List.copyOf(items);
        }

        public synchronized List<Failure> failures() {
            return List.copyOf(failures);
        }
    }

}
