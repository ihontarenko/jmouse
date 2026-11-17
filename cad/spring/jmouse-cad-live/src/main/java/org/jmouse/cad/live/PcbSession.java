package org.jmouse.cad.live;

import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.json.patch.JsonDifference;
import org.jmouse.json.patch.JsonPatch;
import org.jmouse.json.patch.JsonPatchRefusal;
import org.jmouse.websocket.SocketConversation;
import org.jmouse.websocket.SocketOutput;
import org.jmouse.websocket.SocketPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * An open pcb's conversation with the engine, over one socket ({@code cad.pcb}): the editor sends a layout once, then only what
 * changed in it, and asks what the REST routes answer — a gesture's step, what a release lays, the copper and the checks.
 *
 * <h3>Messages</h3>
 * <pre>
 * → { type: ask, id, kind, board?, request: {…}, layouts: { field: { slot, whole | patch } } }
 * → { type: abandon, id }
 * ← { type: answer, id, kind, whole | patch }       patch: what changed since the last answer of its kind on this socket
 * ← { type: refused, id, status, code, detail }       code layout-unknown: send that slot whole again
 * ← { type: dropped, id }                             a newer ask of its kind came first, or the editor abandoned it
 * </pre>
 *
 * <h3>Layouts kept by slot</h3>
 * <p>The session keeps each layout the editor named — {@code before} a gesture, {@code moved} by it, the pcb as it stands — as
 * a JSON tree under its slot. An ask sends a slot whole, or a JSON Patch (RFC 6902) against what the slot holds, and the
 * request field is filled from it: a part carried over a board of a thousand tracks sends a few operations, not the board twice.
 * A patch is applied to a copy, so an ask already waiting keeps the layout it was asked with.</p>
 *
 * <h3>Answers as what changed</h3>
 * <p>An answer of a kind is sent whole the first time on a socket, and after that as a JSON Patch against the last answer of
 * that kind sent over it: tracks a step moved, the pours a change reached — never the board's every track and pour again. The
 * editor keeps the last of each kind as the engine does, so both apply the same patch to the same document.</p>
 *
 * <h3>The newest wins, but not over what was done</h3>
 * <p>One ask is worked on at a time, a session's asks in turn ({@link WaitingAsks}); while one is, the newest ask of a kind waits
 * and an older one of that kind is dropped — what a pointer asked a moment ago no longer matters once it has moved on —, except a
 * kind that is something the reader did, a corner clicked: every one of those is answered, in the order asked.</p>
 */
final class PcbSession implements SocketConversation {

    private static final Logger LOGGER = LoggerFactory.getLogger(PcbSession.class);

    private final PcbAsks         asks;
    private final SocketPrincipal principal;
    private final ObjectMapper    mapper;
    private final SocketOutput    output;

    /** The layouts the editor sent, by slot: read and written on the socket's own thread alone. */
    private final Map<String, JsonNode> layouts = new HashMap<>();

    /** The asks waiting their turn. */
    private final WaitingAsks waiting = new WaitingAsks();
    /** The last answer of each kind sent over this socket: what the next one is sent as a change of. */
    private final Map<PcbAskKind, JsonNode> answered = new EnumMap<>(PcbAskKind.class);
    private boolean                        working;
    private boolean                        closed;

    PcbSession(PcbAsks asks, SocketPrincipal principal, ObjectMapper mapper, SocketOutput output) {
        this.asks = asks;
        this.principal = principal;
        this.mapper = mapper;
        this.output = output;
    }

    @Override
    public void receive(String text) {
        JsonNode message;
        try {
            message = mapper.readTree(text);
        } catch (RuntimeException unreadable) {
            reply(refusal(-1, 400, "unreadable", "The message was not understood: " + unreadable.getMessage()));
            return;
        }
        long id = message.path("id").asLong(-1);
        switch (message.path("type").asString("")) {
            case "ask" -> asked(id, message);
            case "abandon" -> abandoned(id);
            default -> reply(refusal(id, 400, "unreadable", "Not a message of a pcb's session: " + message.path("type")));
        }
    }

    private void asked(long id, JsonNode message) {
        PcbAskKind kind = PcbAskKind.of(message.path("kind").asString("")).orElse(null);
        if (kind == null) {
            reply(refusal(id, 400, "unreadable", "Not something a pcb's session answers: " + message.path("kind")));
            return;
        }
        ObjectNode request = message.path("request").isObject() ? (ObjectNode) message.get("request").deepCopy() : mapper.createObjectNode();
        for (Map.Entry<String, JsonNode> field : message.path("layouts").properties()) {
            JsonNode sent = field.getValue();
            String slot = sent.path("slot").asString(field.getKey());
            JsonNode layout;
            if (sent.has("whole")) {
                layout = sent.get("whole");
            } else {
                JsonNode kept = layouts.get(slot);
                if (kept == null) {
                    reply(refusal(id, 409, "layout-unknown", "The session holds no layout " + slot + ": send it whole.").put("slot", slot));
                    return;
                }
                try {
                    layout = JsonPatch.of(sent.path("patch")).appliedTo(kept);
                } catch (JsonPatchRefusal refused) {
                    layouts.remove(slot);
                    reply(refusal(id, 409, "layout-unknown", "The layout " + slot + " is not the one the change was made to (" + refused.getMessage()
                                                             + "): send it whole.").put("slot", slot));
                    return;
                }
            }
            layouts.put(slot, layout);
            request.set(field.getKey(), layout);
        }
        String board = message.path("board").isString() ? message.get("board").asString() : null;
        enqueue(new WaitingAsks.Waiting(id, kind, board, request));
    }

    private void abandoned(long id) {
        boolean dropped;
        synchronized (this) {
            dropped = waiting.abandon(id);
        }
        if (dropped) {
            reply(dropped(id));
        }
    }

    private void enqueue(WaitingAsks.Waiting ask) {
        Optional<WaitingAsks.Waiting> older;
        boolean start;
        synchronized (this) {
            if (closed) {
                return;
            }
            older = waiting.add(ask);
            start = !working;
            working = true;
        }
        older.ifPresent(dropped -> reply(dropped(dropped.id())));
        if (start) {
            Thread.ofVirtual().name("pcb-session").start(this::work);
        }
    }

    /** Works on the waiting asks in turn until none is left. */
    private void work() {
        for (;;) {
            Optional<WaitingAsks.Waiting> next;
            synchronized (this) {
                next = closed ? Optional.empty() : waiting.next();
                if (next.isEmpty()) {
                    working = false;
                    return;
                }
            }
            reply(answerTo(next.get()));
        }
    }

    private ObjectNode answerTo(WaitingAsks.Waiting ask) {
        try {
            JsonNode answer = mapper.valueToTree(principal.runAs(() -> asks.answer(ask.kind(), ask.board(), ask.request())));
            ObjectNode reply = mapper.createObjectNode().put("type", "answer").put("id", ask.id()).put("kind", ask.kind().word());
            JsonNode last;
            synchronized (this) {
                last = answered.put(ask.kind(), answer);
            }
            if (last == null) {
                reply.set("whole", answer);
            } else {
                reply.set("patch", JsonDifference.between(last, answer).written(mapper.getNodeFactory()));
            }
            return reply;
        } catch (CadProblem problem) {
            ObjectNode refusal = refusal(ask.id(), problem.status(), problem.code(), problem.getMessage());
            problem.details().forEach((name, value) -> refusal.set(name, mapper.valueToTree(value)));
            return refusal;
        } catch (IllegalArgumentException | IllegalStateException | tools.jackson.core.JacksonException unreadable) {
            return refusal(ask.id(), 400, "invalid", unreadable.getMessage());
        } catch (RuntimeException defect) {
            LOGGER.error("A pcb session's {} failed", ask.kind().word(), defect);
            return refusal(ask.id(), 500, "defect", "The engine failed on it: " + defect);
        }
    }

    private ObjectNode refusal(long id, int status, String code, String detail) {
        return mapper.createObjectNode().put("type", "refused").put("id", id).put("status", status).put("code", code)
                .put("detail", detail == null ? "Refused." : detail);
    }

    private ObjectNode dropped(long id) {
        return mapper.createObjectNode().put("type", "dropped").put("id", id);
    }

    private void reply(ObjectNode message) {
        output.text(mapper.writeValueAsString(message));
    }

    @Override
    public void close() {
        synchronized (this) {
            closed = true;
            waiting.clear();
            answered.clear();
        }
        layouts.clear();
    }
}
