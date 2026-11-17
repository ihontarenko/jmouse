package org.jmouse.cad.live;

import tools.jackson.databind.JsonNode;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The asks of a pcb's session waiting their turn: by kind, in the order their kinds were first asked, each kind as its
 * {@link PcbAskKind.Waiting} says — the newest alone, or every one in the order asked. Not safe across threads: the session
 * guards it.
 */
final class WaitingAsks {

    /** An ask whose request is whole, waiting to be worked on. */
    record Waiting(long id, PcbAskKind kind, String board, JsonNode request) {
    }

    private final Map<PcbAskKind, Deque<Waiting>> byKind = new LinkedHashMap<>();

    /** An ask added to wait its turn; the one of its kind it drops, where a newer ask of its kind drops the one waiting. */
    Optional<Waiting> add(Waiting ask) {
        Deque<Waiting> ofKind = byKind.computeIfAbsent(ask.kind(), kind -> new ArrayDeque<>());
        Optional<Waiting> dropped = ask.kind().waiting() == PcbAskKind.Waiting.NEWEST ? Optional.ofNullable(ofKind.pollFirst()) : Optional.empty();
        ofKind.addLast(ask);
        return dropped;
    }

    /** The ask whose turn it is, taken off: the first of the kind asked first. */
    Optional<Waiting> next() {
        Iterator<Map.Entry<PcbAskKind, Deque<Waiting>>> kinds = byKind.entrySet().iterator();
        if (!kinds.hasNext()) {
            return Optional.empty();
        }
        Deque<Waiting> first = kinds.next().getValue();
        Waiting next = first.pollFirst();
        if (first.isEmpty()) {
            kinds.remove();
        }
        return Optional.ofNullable(next);
    }

    /** Whether an ask was waiting, taken off: the editor no longer wants it. */
    boolean abandon(long id) {
        boolean found = false;
        for (Iterator<Deque<Waiting>> kinds = byKind.values().iterator(); kinds.hasNext(); ) {
            Deque<Waiting> ofKind = kinds.next();
            found |= ofKind.removeIf(ask -> ask.id() == id);
            if (ofKind.isEmpty()) {
                kinds.remove();
            }
        }
        return found;
    }

    void clear() {
        byKind.clear();
    }
}
