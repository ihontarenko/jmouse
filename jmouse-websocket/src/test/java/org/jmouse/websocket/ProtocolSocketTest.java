package org.jmouse.websocket;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProtocolSocketTest {

    private static SocketProtocol named(String name) {
        return new SocketProtocol() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public int maximumMessageCharacters() {
                return 1024;
            }

            @Override
            public SocketConversation open(SocketPrincipal principal, SocketOutput output) {
                throw new UnsupportedOperationException("not opened in this test");
            }
        };
    }

    @Test
    void readsTheTokenOfferedAsASubprotocol() {
        assertEquals(Optional.of("abc.def"), ProtocolSocket.tokenOf(List.of("cad.pcb, bearer.abc.def")));
        assertEquals(Optional.of("x"), ProtocolSocket.tokenOf(List.of("cad.live", "bearer.x")));
        assertEquals(Optional.empty(), ProtocolSocket.tokenOf(List.of("cad.live")));
        assertEquals(Optional.empty(), ProtocolSocket.tokenOf(List.of("bearer." + "t".repeat(9000))));
    }

    @Test
    void agreesTheFirstProtocolAskedThatItHolds() {
        ProtocolSocket.Configurator configurator = new ProtocolSocket.Configurator(List.of(named("cad.live"), named("cad.pcb")), token -> Optional.empty());
        assertEquals("cad.pcb", configurator.getNegotiatedSubprotocol(List.of(), List.of("cad.pcb", "bearer.token")));
        assertEquals("cad.live", configurator.getNegotiatedSubprotocol(List.of(), List.of("bearer.token", "cad.live")));
        assertEquals("", configurator.getNegotiatedSubprotocol(List.of(), List.of("chat", "bearer.token")));
    }

    @Test
    void refusesTwoProtocolsOfOneName() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProtocolSocket.Configurator(List.of(named("cad.live"), named("cad.live")), token -> Optional.empty()));
    }
}
