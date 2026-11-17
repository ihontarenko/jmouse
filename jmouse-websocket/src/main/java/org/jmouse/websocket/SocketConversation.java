package org.jmouse.websocket;

/**
 * 💬 One conversation over one socket: what its protocol does with each text message the reader sends, and what it lets go
 * of when the socket closes. Messages arrive one at a time, in the order sent.
 */
public interface SocketConversation {

    /** A text message the reader sent. */
    void receive(String message);

    /** The socket closed — or failed —: called once, whatever ended it. */
    void close();
}
