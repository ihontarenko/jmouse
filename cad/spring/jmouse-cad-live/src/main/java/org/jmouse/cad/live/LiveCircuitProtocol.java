package org.jmouse.cad.live;

import org.jmouse.websocket.SocketConversation;
import org.jmouse.websocket.SocketOutput;
import org.jmouse.websocket.SocketPrincipal;
import org.jmouse.websocket.SocketProtocol;
import tools.jackson.databind.ObjectMapper;

/** {@code cad.live}: one running circuit a socket ({@link LiveRun}), counted while it is open. */
final class LiveCircuitProtocol implements SocketProtocol {

    static final String NAME = "cad.live";
    /** A {@code start} or a {@code restart} carries the whole circuit, its layout included: a circuit and its sheet, with room to spare. */
    private static final int MAXIMUM_MESSAGE_CHARACTERS = 2 * 1024 * 1024;

    private final LiveCircuits circuits;
    private final ObjectMapper mapper;

    LiveCircuitProtocol(LiveCircuits circuits, ObjectMapper mapper) {
        this.circuits = circuits;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public int maximumMessageCharacters() {
        return MAXIMUM_MESSAGE_CHARACTERS;
    }

    @Override
    public SocketConversation open(SocketPrincipal principal, SocketOutput output) {
        LiveRun run = new LiveRun(circuits, principal, mapper, output);
        circuits.opened(run);
        return new SocketConversation() {
            @Override
            public void receive(String message) {
                run.receive(message);
            }

            @Override
            public void close() {
                run.close();
                circuits.closed(run);
            }
        };
    }
}
