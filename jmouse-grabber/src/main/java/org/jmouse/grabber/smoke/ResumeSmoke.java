package org.jmouse.grabber.smoke;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

import org.jmouse.core.matcher.TextMatchers;
import org.jmouse.grabber.GrabListener;
import org.jmouse.grabber.GrabRun;
import org.jmouse.grabber.Grabber;
import org.jmouse.grabber.ItemSink;
import org.jmouse.grabber.journal.VisitJournal;

/**
 * 🧪 The smoke that matters: a run killed halfway does not redo its work.
 *
 * <p>The brief calls resume a <i>критично важлива вимога</i>, and a requirement nobody demonstrated is
 * a requirement nobody has. So this runs the same crawl twice against one journal on disk, stopping
 * the first run after two pages, and the assertion is that the second run fetches only what the first
 * did not.</p>
 *
 * <p>⚠️ Concurrency is deliberately one. With four workers in flight the stop lands after however many
 * were already running, and a demonstration whose number moves is not a demonstration.</p>
 */
public final class ResumeSmoke {

    private static final int STOP_AFTER = 2;

    public static void main(String[] arguments) throws IOException {
        Path journalDirectory = Path.of("target", "grabber-resume-smoke");

        deleteTree(journalDirectory);

        System.out.println("== first run, stopping itself after " + STOP_AFTER + " pages ==");
        Outcome first = crawl(journalDirectory, STOP_AFTER);

        System.out.println("== second run, against the same journal ==");
        Outcome second = crawl(journalDirectory, Integer.MAX_VALUE);

        System.out.println("== third run, with nothing left to do ==");
        Outcome third = crawl(journalDirectory, Integer.MAX_VALUE);

        long handled = first.handled + second.handled;

        System.out.println("== checks ==");
        check("the first run stopped where it was told", first.handled == STOP_AFTER, first.handled);
        check("between them the two runs handled all five pages", handled == 5, handled);
        check("the second run repeated nothing", second.handled == 5 - STOP_AFTER, second.handled);
        check("a third run has nothing left to do", third.handled == 0, third.handled);
        check("no page was handled twice", first.addresses.stream().noneMatch(second.addresses::contains),
              first.addresses + " then " + second.addresses);
    }

    private static Outcome crawl(Path journalDirectory, int stopAfter) {
        AtomicInteger          fetched   = new AtomicInteger();
        ItemSink.Collecting    collected = ItemSink.collecting();

        try (VisitJournal journal = VisitJournal.onDisk(journalDirectory)) {
            Grabber grabber = Grabber.builder()
                    .fetcher(Fixtures.fetcher())
                    .policy(policy -> policy.concurrency(1).delay(Duration.ZERO))
                    .journal(journal)
                    .seed(Fixtures.CATALOG)
                    .route("listing", TextMatchers.contains("/catalog"), page -> {
                        if (fetched.incrementAndGet() > stopAfter) {
                            return page.stop("seen enough");
                        }

                        page.emit(page.url().toString());

                        return page.follow("a.product-link, .pagination a.next");
                    })
                    .route("product", TextMatchers.contains("/product/"), page -> {
                        if (fetched.incrementAndGet() > stopAfter) {
                            return page.stop("seen enough");
                        }

                        page.emit(page.url().toString());

                        return page.leaf();
                    })
                    .into(collected)
                    .build();

            GrabRun run = grabber.run();

            System.out.println("  " + run);
            collected.items().forEach(item -> System.out.println("    got " + item));

            return new Outcome(collected.size(), collected.itemsOf(String.class));
        } catch (Exception exception) {
            throw new IllegalStateException("The resume smoke could not run", exception);
        }
    }

    /**
     * 📊 What one run did.
     *
     * <p>⚠️ Counted from the SINK rather than from the run's own figures. A page that was fetched and
     * then refused by the stop switch was not work that was done, and the run's {@code fetched}
     * counter cannot tell the difference — which is exactly the confusion this smoke exists to
     * settle.</p>
     */
    private record Outcome(long handled, java.util.List<String> addresses) {
    }

    private static void deleteTree(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        try (var walk = Files.walk(directory)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new IllegalStateException("Cannot clear " + path, exception);
                }
            });
        }
    }

    private static void check(String what, boolean held, Object saw) {
        System.out.println((held ? "  OK   " : "  FAIL ") + what + (held ? "" : " — saw " + saw));
    }

}
