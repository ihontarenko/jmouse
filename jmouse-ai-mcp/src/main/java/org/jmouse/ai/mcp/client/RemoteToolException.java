package org.jmouse.ai.mcp.client;

/**
 * A server this application connected to did not answer.
 *
 * <p><strong>An exception rather than a refusal, and the difference is not pedantry.</strong> Every
 * refusal this library produces ends by promising that nothing was changed. A call that vanished into a
 * refused connection, a timeout or a closed session cannot promise that — the request may have arrived
 * and been carried out with the answer lost on the way back. So this travels as a failure, the
 * dispatcher records it as one, and what a model reads says that part of it may have happened.
 *
 * <p>⚠️ <strong>The server is named in the message, always.</strong> A connection refused to somebody
 * else's machine that reads as <em>"the tool is broken"</em> sends whoever is on call to look at this
 * application's code, and they will find nothing wrong with it.
 */
public class RemoteToolException extends RuntimeException {

    private final String serverName;

    public RemoteToolException(String serverName, String message) {
        super(message);
        this.serverName = serverName;
    }

    public RemoteToolException(String serverName, String message, Throwable cause) {
        super(message, cause);
        this.serverName = serverName;
    }

    /** Which server, so a caller of this does not have to parse the sentence to find out. */
    public String serverName() {
        return serverName;
    }
}
