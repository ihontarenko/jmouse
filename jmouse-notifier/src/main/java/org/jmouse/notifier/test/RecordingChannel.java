package org.jmouse.notifier.test;

import org.jmouse.notifier.Address;
import org.jmouse.notifier.Addressee;
import org.jmouse.notifier.Channel;
import org.jmouse.notifier.DeliveryOutcome;
import org.jmouse.notifier.Publication;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A channel that remembers what it was asked to deliver, so a product can be built and checked with no
 * network at all.
 *
 * <p>Addresses are whatever {@link #reach(String, String...)} said; outcomes are {@code Delivered}
 * unless {@link #willAnswer(DeliveryOutcome)} queued something else — queued, not set, so "later,
 * later, then fine" is expressible.
 *
 * <p>⚠️ In {@code src/main}, not a test jar: this repository has no JUnit, and a consuming product needs
 * the fake on its own compile path.
 */
public final class RecordingChannel implements Channel {

    private final String                    name;
    private final Map<String, List<String>> reach    = new HashMap<>();
    private final Deque<DeliveryOutcome>    answers  = new ArrayDeque<>();
    private final List<Call>                calls    = new ArrayList<>();

    public RecordingChannel(String name) {
        this.name = name;
    }

    /** One attempt this channel was asked to make. */
    public record Call(Publication publication, Address address) {
    }

    /** Says where a subject is reachable on this channel. */
    public synchronized RecordingChannel reach(String subject, String... values) {
        reach.computeIfAbsent(subject, ignored -> new ArrayList<>()).addAll(List.of(values));
        return this;
    }

    /** Queues the outcome of the next attempt. */
    public synchronized RecordingChannel willAnswer(DeliveryOutcome outcome) {
        answers.addLast(outcome);
        return this;
    }

    public synchronized List<Call> calls() {
        return List.copyOf(calls);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public synchronized List<Address> addressesOf(Addressee addressee) {
        return reach.getOrDefault(addressee.subject(), List.of()).stream()
                    .map(value -> new Address(name, value))
                    .toList();
    }

    @Override
    public synchronized DeliveryOutcome deliver(Publication publication, Address address) {
        calls.add(new Call(publication, address));

        DeliveryOutcome queued = answers.pollFirst();

        return queued != null ? queued : new DeliveryOutcome.Delivered(address.value() + "#" + calls.size());
    }
}
