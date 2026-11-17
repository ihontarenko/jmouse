package org.jmouse.cad.live;

import org.jmouse.websocket.SocketConversation;
import org.jmouse.websocket.SocketOutput;
import org.jmouse.websocket.SocketPrincipal;
import org.jmouse.websocket.SocketProtocol;
import tools.jackson.databind.ObjectMapper;

/** {@code cad.pcb}: one open pcb's session a socket ({@link PcbSession}). */
final class LivePcbProtocol implements SocketProtocol {

    static final String NAME = "cad.pcb";
    /** The first ask carries the pcb's text and its layout whole: a large board's, with room to spare. */
    private static final int MAXIMUM_MESSAGE_CHARACTERS = 16 * 1024 * 1024;

    private final PcbAsks      asks;
    private final ObjectMapper mapper;

    LivePcbProtocol(PcbAsks asks, ObjectMapper mapper) {
        this.asks = asks;
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
        return new PcbSession(asks, principal, mapper, output);
    }
}
