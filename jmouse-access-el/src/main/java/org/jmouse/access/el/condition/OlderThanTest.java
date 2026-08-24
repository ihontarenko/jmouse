package org.jmouse.access.el.condition;

import java.time.Clock;
import java.time.Duration;

/**
 * {@code X is olderThan('30d')} — whether the moment on the left is further back than the span.
 *
 * <pre>
 * &#64;SPACE:'id-space-01' invoice:void deny     when resource.createdAt is olderThan('30d')
 * &#64;SPACE:'id-space-01' document:restore deny when resource.deletedAt is olderThan('90d')
 * </pre>
 *
 * <p>Retention floors, closed periods, anything that stops being changeable once it is old enough.
 *
 * <p>⚠️ Strictly older: a moment exactly the span ago does <strong>not</strong> hold, so
 * {@code olderThan} and {@link WithinTest} partition an ordinary timeline between them without an
 * overlap. The boundary matters to nobody and its absence would matter to somebody.
 *
 * <p>The rest — what happens to a future moment, an unreadable one and an absent one — is on
 * {@link AgeTest}, and is worth reading before writing a rule with either.
 */
public class OlderThanTest extends AgeTest {

    public static final String NAME = "olderThan";

    public OlderThanTest() {
        this(Clock.systemDefaultZone());
    }

    public OlderThanTest(Clock clock) {
        super(clock);
    }

    @Override
    protected boolean decide(Duration elapsed, Duration span) {
        return elapsed.compareTo(span) > 0;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
