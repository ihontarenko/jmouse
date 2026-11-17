package org.jmouse.cad.live;

import org.jmouse.websocket.SocketPrincipal;

/**
 * Who a live socket runs for — a running circuit, an open pcb's session —: every message is run as this actor and decided by
 * the host's {@code CadAuthority} exactly as a request of theirs would be. jmCAD's name for the socket's principal, so a
 * product implements jmCAD's types and none of the socket library's.
 */
public interface LivePrincipal extends SocketPrincipal {
}
