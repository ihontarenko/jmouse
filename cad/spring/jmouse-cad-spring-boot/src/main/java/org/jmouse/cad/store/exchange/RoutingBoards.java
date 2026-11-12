package org.jmouse.cad.store.exchange;

import org.jmouse.cad.document.DocumentOwner;
import org.jmouse.cad.pcb.routing.RoutingEngine;
import org.jmouse.cad.store.problem.CadProblem;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The pcbs an editor is laying copper on, each read once: a board is opened for a pcb's text as it stands, and every gesture
 * after names it, so a pointer's every move costs the routing alone and never the reading of the pcb again. A board is named by
 * what it was opened from — the same text, the same reader, the same board —, and the least recently asked go first.
 */
public final class RoutingBoards {

    private final DocumentExchange              exchange;
    private final Map<String, RoutingEngine>    boards;

    /** @param kept how many boards are kept open at once */
    public RoutingBoards(DocumentExchange exchange, int kept) {
        this.exchange = exchange;
        this.boards = new LinkedHashMap<>(kept, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, RoutingEngine> eldest) {
                return size() > kept;
            }
        };
    }

    /** The board for a pcb's text, opened when it is not yet: its name. */
    public String open(String source, DocumentOwner owner) {
        String name = nameOf(source, owner);
        synchronized (boards) {
            if (boards.containsKey(name)) {
                return name;
            }
        }
        RoutingEngine engine = new RoutingEngine(exchange.routingBoard(source, owner));
        synchronized (boards) {
            boards.putIfAbsent(name, engine);
        }
        return name;
    }

    /** The router of an open board; refused as not found where it was never opened or has been let go — the editor opens it again. */
    public RoutingEngine engineOf(String name) {
        synchronized (boards) {
            RoutingEngine engine = boards.get(name);
            if (engine == null) {
                throw CadProblem.notFound("routing board " + name);
            }
            return engine;
        }
    }

    private static String nameOf(String source, DocumentOwner owner) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update((owner == null ? "" : owner.type() + ":" + owner.id()).getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException missing) {
            throw new IllegalStateException("SHA-256 is part of every Java platform", missing);
        }
    }
}
