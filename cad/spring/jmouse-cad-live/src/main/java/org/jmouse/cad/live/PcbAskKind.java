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

    OPEN(Waiting.NEWEST, Lane.ROUTING),
    /** The rest of a track toward the pointer, as it moves: only where the pointer is now matters. */
    LEG(Waiting.NEWEST, Lane.ROUTING),
    /** A leg to a corner the reader clicked: every one is laid, in the order clicked — the next goes on from it. */
    CLICKED_LEG(Waiting.EVERY, Lane.ROUTING),
    SEGMENT(Waiting.NEWEST, Lane.ROUTING),
    CORNER(Waiting.NEWEST, Lane.ROUTING),
    FOLLOWING(Waiting.NEWEST, Lane.ROUTING),
    LAID(Waiting.NEWEST, Lane.ROUTING),
    JOGS(Waiting.NEWEST, Lane.RESTING),
    CORNERS(Waiting.NEWEST, Lane.RESTING),
    /** Where the copper's lines end and meet, for the pcb as it stands now. */
    NODES(Waiting.NEWEST, Lane.RESTING),
    TUNE(Waiting.NEWEST, Lane.ROUTING),
    TUNE_TRACK(Waiting.NEWEST, Lane.ROUTING),
    DERIVED(Waiting.NEWEST, Lane.DERIVED);

    /** How asks of a kind wait their turn. */
    enum Waiting {
        /** A newer ask drops the one still waiting: what a pointer asked a moment ago no longer matters once it has moved on. */
        NEWEST,
        /** Every ask is answered, in the order asked: each is something the reader did, never superseded by the next. */
        EVERY
    }

    /**
     * Where asks of a kind are worked on: each lane its own thread, its asks in turn, so what one lane takes long over never holds
     * another up. The router's asks share one, for they share the board's engine; what is derived of a pcb — its pours, its checks —
     * goes on beside them, never between a hand and its track.
     */
    enum Lane {
        /** What the hand waits on, answered by the board's gestures' engine: a track laid or carried, a part followed. */
        ROUTING,
        /** What the pcb is looked over for once it rests — its nodes, its jogs, its corners —, by an engine of its own over the board. */
        RESTING,
        /** What is derived of the pcb as it stands — the copper poured, the checks —: worked out beside the routing, never before it. */
        DERIVED
    }

    private final Waiting waiting;
    private final Lane    lane;

    PcbAskKind(Waiting waiting, Lane lane) {
        this.waiting = waiting;
        this.lane = lane;
    }

    Waiting waiting() {
        return waiting;
    }

    Lane lane() {
        return lane;
    }

    /** How it is written in a message: {@code tune-track}. */
    String word() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    static Optional<PcbAskKind> of(String word) {
        return Arrays.stream(values()).filter(kind -> kind.word().equals(word)).findFirst();
    }
}
