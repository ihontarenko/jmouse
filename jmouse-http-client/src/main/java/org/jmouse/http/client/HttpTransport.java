package org.jmouse.http.client;

/**
 * What sends a call and brings its answer back: the one seam every call out goes through, so a test gives an answer
 * without a network and a product chooses the client underneath. {@link JdkHttpTransport} is the first.
 */
@FunctionalInterface
public interface HttpTransport {

    /**
     * The call's answer, whatever its status.
     *
     * @throws HttpCallException when it got none: too slow, too long, the connection failed, the wait interrupted
     */
    HttpReply exchange(HttpCall call);
}
