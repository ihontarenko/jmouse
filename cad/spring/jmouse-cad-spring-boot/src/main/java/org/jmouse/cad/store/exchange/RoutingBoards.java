package org.jmouse.cad.store.exchange;

import org.jmouse.cad.document.DocumentOwner;
import org.jmouse.cad.pcb.routing.RoutingBoard;
import org.jmouse.cad.pcb.routing.RoutingEngine;
import org.jmouse.cad.store.problem.CadProblem;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The pcbs an editor is laying copper on, each read once: a board is opened for a pcb's text as it stands, and every gesture
 * after names it, so a pointer's every move costs the routing alone and never the reading of the pcb again. A board is named by
 * what it was opened from — the same text, the same reader, the same board —, and the least recently asked go first. Each kind of
 * routing work over a board has an engine of its own ({@link RoutingWork}), made from the board read once.
 */
public final class RoutingBoards {

    private final DocumentExchange              exchange;
    private final Map<String, OpenBoard>        boards;

    /** A board read once, and an engine for each kind of work over it, made as it is first asked for. */
    private static final class OpenBoard {

        private final RoutingBoard                       board;
        private final Map<RoutingWork, RoutingEngine>    engines = new EnumMap<>(RoutingWork.class);

        OpenBoard(RoutingBoard board) {
            this.board = board;
        }

        synchronized RoutingEngine engineFor(RoutingWork work) {
            return engines.computeIfAbsent(work, ignored -> new RoutingEngine(board));
        }
    }

    /** @param kept how many boards are kept open at once */
    public RoutingBoards(DocumentExchange exchange, int kept) {
        this.exchange = exchange;
        this.boards = new LinkedHashMap<>(kept, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, OpenBoard> eldest) {
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
        OpenBoard opened = new OpenBoard(exchange.routingBoard(source, owner));
        // The gestures' engine made at once, as it always was: the first gesture finds it ready.
        opened.engineFor(RoutingWork.GESTURES);
        synchronized (boards) {
            boards.putIfAbsent(name, opened);
        }
        return name;
    }

    /** The router of an open board for gestures; refused as not found where it was never opened or has been let go. */
    public RoutingEngine engineOf(String name) {
        return engineOf(name, RoutingWork.GESTURES);
    }

    /** The router of an open board for a kind of work; refused as not found where it was never opened or has been let go — the editor opens it again. */
    public RoutingEngine engineOf(String name, RoutingWork work) {
        OpenBoard opened;
        synchronized (boards) {
            opened = boards.get(name);
        }
        if (opened == null) {
            throw CadProblem.notFound("routing board " + name);
        }
        return opened.engineFor(work);
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
