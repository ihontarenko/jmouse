package org.jmouse.cad.live;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * What an open pcb's session may ask: the router's board opened, a gesture's step, what a release lays, the copper and checks
 * made of the pcb. Each is the REST route of the same name's own work ({@link PcbAsks}); how one still waiting meets a newer one
 * of its kind is the kind's {@link Waiting}.
 */
enum PcbAskKind {

    OPEN(Waiting.NEWEST),
    /** The rest of a track toward the pointer, as it moves: only where the pointer is now matters. */
    LEG(Waiting.NEWEST),
    /** A leg to a corner the reader clicked: every one is laid, in the order clicked — the next goes on from it. */
    CLICKED_LEG(Waiting.EVERY),
    SEGMENT(Waiting.NEWEST),
    CORNER(Waiting.NEWEST),
    FOLLOWING(Waiting.NEWEST),
    LAID(Waiting.NEWEST),
    JOGS(Waiting.NEWEST),
    CORNERS(Waiting.NEWEST),
    /** Where the copper's lines end and meet, for the pcb as it stands now. */
    NODES(Waiting.NEWEST),
    TUNE(Waiting.NEWEST),
    TUNE_TRACK(Waiting.NEWEST),
    DERIVED(Waiting.NEWEST);

    /** How asks of a kind wait their turn. */
    enum Waiting {
        /** A newer ask drops the one still waiting: what a pointer asked a moment ago no longer matters once it has moved on. */
        NEWEST,
        /** Every ask is answered, in the order asked: each is something the reader did, never superseded by the next. */
        EVERY
    }

    private final Waiting waiting;

    PcbAskKind(Waiting waiting) {
        this.waiting = waiting;
    }

    Waiting waiting() {
        return waiting;
    }

    /** How it is written in a message: {@code tune-track}. */
    String word() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    static Optional<PcbAskKind> of(String word) {
        return Arrays.stream(values()).filter(kind -> kind.word().equals(word)).findFirst();
    }
}
