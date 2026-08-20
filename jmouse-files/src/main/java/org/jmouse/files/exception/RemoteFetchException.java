package org.jmouse.files.exception;

/**
 * 🌐 A file could not be fetched from the address it was asked for.
 *
 * <p>Every reason is the caller's or the remote server's rather than this installation's: an address
 * that does not parse, a host that does not resolve, a host that resolves <em>inside</em> this network,
 * an error status, an HTML page where a file was expected, or a declared size over the limit. All of
 * them are a bad request, and all of them say which.</p>
 */
public class RemoteFetchException extends RuntimeException {

    /**
     * 🏗️ Refuse a fetch.
     *
     * @param message what went wrong, phrased for whoever pasted the address
     */
    public RemoteFetchException(String message) {
        super(message);
    }
}
