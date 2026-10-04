package org.jmouse.telegram.bot.smoke;

import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.IdentitySource;
import org.jmouse.telegram.TelegramException;
import org.jmouse.telegram.TelegramIdentity;
import org.jmouse.telegram.TelegramRefusal;
import org.jmouse.telegram.bot.LongPolling;
import org.jmouse.telegram.bot.UpdateFetcher;
import org.jmouse.telegram.smoke.Checks;
import org.jmouse.telegram.update.IncomingMessage;
import org.jmouse.telegram.update.Update;
import org.jmouse.telegram.update.UpdateDispatcher;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import static org.jmouse.telegram.smoke.Checks.check;

/**
 * The polling loop, driven by a scripted {@link UpdateFetcher} rather than by Telegram.
 *
 * <p>What is worth checking here is not that an HTTP call works — it is the behaviour around it:
 * <strong>when the offset advances</strong>, that a wrong token stops the loop instead of retrying
 * forever, that an outage does not, and that closing it actually stops. None of that is reachable
 * against real Telegram on purpose, and the offset rule is the one whose failure silently loses
 * messages.
 *
 * <p>Run its {@code main}.
 */
public final class PollingSmoke {

    public static void main(String[] arguments) throws InterruptedException {
        advancesTheOffsetOnlyPastWhatItDispatched();
        doesNotAdvanceWhenNothingArrives();
        stopsOnAWrongToken();
        survivesAnOutageAndResumes();
        stopsPromptlyWhenClosed();

        Checks.report();
    }

    private static void advancesTheOffsetOnlyPastWhatItDispatched() throws InterruptedException {
        List<Long>       dispatched = new CopyOnWriteArrayList<>();
        UpdateDispatcher dispatcher = new UpdateDispatcher();

        dispatcher.on(Update.MessageReceived.class, update -> dispatched.add(update.updateId()));

        ScriptedFetcher fetcher = new ScriptedFetcher();

        fetcher.willReturn(List.of(message(100), message(101), message(102)));

        LongPolling polling = polling(fetcher, dispatcher);

        polling.start();
        fetcher.awaitCalls(2);
        polling.close();

        check("every update reached the dispatcher",
                dispatched.equals(List.of(100L, 101L, 102L)));

        // ⚠️ The rule the whole loop turns on: the next request acknowledges everything below the
        // offset, so it may only ever be raised after the batch has been handed over.
        check("the offset is one past the highest dispatched id", polling.offset() == 103);

        check("the first request asked for no offset", fetcher.offsets().get(0) == 0);
        check("the second asked for 103", fetcher.offsets().get(1) == 103);
    }

    private static void doesNotAdvanceWhenNothingArrives() throws InterruptedException {
        ScriptedFetcher fetcher = new ScriptedFetcher();

        fetcher.willReturn(List.of());
        fetcher.willReturn(List.of());

        LongPolling polling = polling(fetcher, new UpdateDispatcher());

        polling.start();
        fetcher.awaitCalls(2);
        polling.close();

        check("an empty poll leaves the offset alone", polling.offset() == 0);
    }

    private static void stopsOnAWrongToken() throws InterruptedException {
        // ⚠️ A revoked token will never start working. Retrying it every five seconds produces a log
        // nobody can read past, and hides the one thing an administrator needs to see.
        ScriptedFetcher fetcher = new ScriptedFetcher();

        fetcher.willRefuse(new TelegramRefusal.Unauthorized("bot", "token revoked"));

        LongPolling polling = polling(fetcher, new UpdateDispatcher());

        polling.start();
        waitUntil(() -> !polling.isRunning(), Duration.ofSeconds(2));

        check("an Unauthorized refusal stops the loop", !polling.isRunning());
        check("it did not keep asking", fetcher.calls() == 1);

        polling.close();
    }

    private static void survivesAnOutageAndResumes() throws InterruptedException {
        ScriptedFetcher fetcher = new ScriptedFetcher();

        fetcher.willRefuse(new TelegramRefusal.TransportFailure("connection refused"));
        fetcher.willReturn(List.of(message(7)));

        List<Long>       dispatched = new CopyOnWriteArrayList<>();
        UpdateDispatcher dispatcher = new UpdateDispatcher();

        dispatcher.on(Update.MessageReceived.class, update -> dispatched.add(update.updateId()));

        LongPolling polling = polling(fetcher, dispatcher);

        polling.start();
        waitUntil(() -> !dispatched.isEmpty(), Duration.ofSeconds(3));
        polling.close();

        check("an outage does not stop the loop", dispatched.equals(List.of(7L)));
    }

    private static void stopsPromptlyWhenClosed() throws InterruptedException {
        // The loop spends its life blocked in a request Telegram holds open, so shutdown has to
        // interrupt rather than only set a flag - otherwise a restart leaves the old poller stealing
        // updates from the new one.
        ScriptedFetcher fetcher = new ScriptedFetcher();

        fetcher.blockForever();

        LongPolling polling = polling(fetcher, new UpdateDispatcher());

        polling.start();
        fetcher.awaitCalls(1);

        long started = System.currentTimeMillis();

        polling.close();

        long elapsed = System.currentTimeMillis() - started;

        check("closing interrupts a blocked poll rather than waiting it out", elapsed < 2000);
        check("it reports itself stopped", !polling.isRunning());
    }

    private static LongPolling polling(UpdateFetcher fetcher, UpdateDispatcher dispatcher) {
        return new LongPolling(
                fetcher,
                IdentitySource.fixed(TelegramIdentity.bot("smoke", "token")),
                dispatcher,
                IdentitySource.GENERAL,
                Duration.ofMillis(50),
                Duration.ofMillis(100),
                java.util.Set.of());
    }

    private static Update message(long updateId) {
        return new Update.MessageReceived(updateId, new IncomingMessage(
                ChatReference.of(1L), (int) updateId, null, "hello", null,
                Instant.now(), List.of(), null));
    }

    private static void waitUntil(java.util.function.BooleanSupplier condition, Duration limit)
            throws InterruptedException {

        long deadline = System.currentTimeMillis() + limit.toMillis();

        while (System.currentTimeMillis() < deadline && !condition.getAsBoolean()) {
            Thread.sleep(10);
        }
    }

    /** A fetcher that answers from a script and records what it was asked for. */
    private static final class ScriptedFetcher implements UpdateFetcher {

        private final Queue<Object> script  = new ConcurrentLinkedQueue<>();
        private final List<Long>    offsets = new CopyOnWriteArrayList<>();
        private final AtomicLong    calls   = new AtomicLong();

        private volatile boolean blocking;

        void willReturn(List<Update> updates) {
            script.add(new ArrayList<>(updates));
        }

        void willRefuse(TelegramRefusal refusal) {
            script.add(refusal);
        }

        void blockForever() {
            blocking = true;
        }

        long calls() {
            return calls.get();
        }

        List<Long> offsets() {
            return List.copyOf(offsets);
        }

        void awaitCalls(int wanted) throws InterruptedException {
            long deadline = System.currentTimeMillis() + 3000;

            while (calls.get() < wanted && System.currentTimeMillis() < deadline) {
                Thread.sleep(5);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<Update> fetchUpdates(
                TelegramIdentity identity, long offset, Duration pollTimeout, java.util.Set<String> allowed) {

            offsets.add(offset);
            calls.incrementAndGet();

            if (blocking) {
                try {
                    Thread.sleep(Duration.ofMinutes(5).toMillis());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new TelegramException(
                            new TelegramRefusal.TransportFailure("the call was interrupted"));
                }
            }

            Object next = script.poll();

            if (next instanceof TelegramRefusal refusal) {
                throw new TelegramException(refusal);
            }

            if (next instanceof List<?> updates) {
                return (List<Update>) updates;
            }

            // Nothing scripted left: behave like an idle bot rather than ending the loop.
            try {
                Thread.sleep(20);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }

            return List.of();
        }
    }

    private PollingSmoke() {
    }
}
