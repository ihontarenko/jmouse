package org.jmouse.websocket;

import java.nio.ByteBuffer;

/**
 * 📤 Where a conversation's messages go: text and binary, sent one at a time whatever thread sends them; a socket that is gone
 * takes them silently.
 */
public interface SocketOutput {

    void text(String message);

    void binary(ByteBuffer frame);
}
