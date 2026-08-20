package org.jmouse.files.management;

import org.jmouse.files.exception.RemoteFetchException;
import org.jmouse.storage.policy.UploadPolicy;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

/**
 * 🌐 Downloads a file from a web address on somebody's behalf.
 *
 * <p>Owns every network-side concern of importing: the SSRF guard against internal addresses, timeouts,
 * response sanity checks, filename derivation, and the early size refusal based on the declared
 * {@code Content-Length}. What happens to the bytes afterwards is {@link FileManagement}'s.</p>
 *
 * <h3>⚠️ An import is judged by the same acceptance policy as an upload</h3>
 *
 * <p>It is the one place the content is chosen by somebody who is not the caller, which makes it the
 * <em>more</em> dangerous of the two rather than the less. This refuses an oversized declaration up
 * front; the real size and type are judged by the ingestion path once the bytes arrive, exactly as they
 * are for a multipart upload.</p>
 *
 * <h3>⚠️ The SSRF guard is the load-bearing part</h3>
 *
 * <p>Without it, "import this URL" is a request that makes <strong>this server</strong> fetch things on
 * the caller's behalf — cloud metadata endpoints, internal admin pages, other services on the same
 * network — and hand the result back. Loopback, site-local, link-local and wildcard addresses are all
 * refused, for every address the host resolves to rather than the first.</p>
 */
public class RemoteFileFetcher {

    private static final int    CONNECT_TIMEOUT_MILLISECONDS = 10_000;
    private static final int    READ_TIMEOUT_MILLISECONDS    = 30_000;
    private static final String ACCEPTED_CONTENT_TYPES       = "application/pdf,application/octet-stream,*/*";

    /** Used when the URL path carries no usable filename; datasheets are the dominant use case. */
    private static final String FALLBACK_FILENAME = "datasheet.pdf";

    private final UploadPolicy uploadPolicy;
    private final String       userAgent;

    /**
     * 🏗️ Build a fetcher that announces itself as this installation.
     *
     * @param uploadPolicy what this installation accepts
     * @param userAgent    what to send as {@code User-Agent} — some servers refuse an unnamed client
     */
    public RemoteFileFetcher(UploadPolicy uploadPolicy, String userAgent) {
        this.uploadPolicy = uploadPolicy;
        this.userAgent    = userAgent;
    }

    /**
     * A successfully opened remote download.
     *
     * @param originalName filename derived from the URL path, or a fallback
     * @param inputStream  the response body; the caller is responsible for closing it
     */
    public record RemoteFile(String originalName, InputStream inputStream) {
    }

    /**
     * 📥 Open {@code sourceUrl} and hand back its name and content stream.
     *
     * @param sourceUrl the address to fetch
     * @return the open download
     * @throws RemoteFetchException when the URL is invalid, resolves to an internal address, answers with
     *                              an error status or an HTML page, declares an oversized
     *                              {@code Content-Length}, or fails on the network
     */
    public RemoteFile fetch(String sourceUrl) {
        try {
            URL url = URI.create(sourceUrl).toURL();

            requireExternalAddress(url);

            URLConnection connection = open(url);

            requireSuccessStatus(connection);
            requireNonHtmlContent(connection);

            // Refuse oversized downloads before spending the bandwidth, when the server admits the size.
            // Servers that omit or understate Content-Length are caught once the real size is known.
            uploadPolicy.ensureWithinSizeLimit(connection.getContentLengthLong());

            return new RemoteFile(filenameFrom(url), connection.getInputStream());
        } catch (RemoteFetchException refusal) {
            throw refusal;
        } catch (IOException | IllegalArgumentException failure) {
            throw new RemoteFetchException("Failed to download file from URL: " + failure.getMessage());
        }
    }

    private URLConnection open(URL url) throws IOException {
        URLConnection connection = url.openConnection();

        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLISECONDS);
        connection.setReadTimeout(READ_TIMEOUT_MILLISECONDS);
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setRequestProperty("Accept", ACCEPTED_CONTENT_TYPES);

        if (connection instanceof HttpURLConnection httpConnection) {
            httpConnection.setInstanceFollowRedirects(true);
        }

        connection.connect();

        return connection;
    }

    private void requireSuccessStatus(URLConnection connection) throws IOException {
        if (connection instanceof HttpURLConnection httpConnection) {
            int responseCode = httpConnection.getResponseCode();

            if (responseCode < 200 || responseCode >= 300) {
                throw new RemoteFetchException("Remote server returned HTTP " + responseCode);
            }
        }
    }

    private void requireNonHtmlContent(URLConnection connection) {
        String contentType = connection.getContentType();

        if (contentType != null && contentType.startsWith("text/html")) {
            throw new RemoteFetchException(
                    "URL returned an HTML page instead of a file — the resource may require "
                    + "authentication or is unavailable");
        }
    }

    /** ⚠️ Guard against SSRF: refuse URLs whose host resolves to a loopback, private or link-local address. */
    private void requireExternalAddress(URL url) {
        try {
            for (InetAddress address : InetAddress.getAllByName(url.getHost())) {
                if (address.isLoopbackAddress() || address.isSiteLocalAddress()
                        || address.isLinkLocalAddress() || address.isAnyLocalAddress()) {
                    throw new RemoteFetchException(
                            "Requests to internal or reserved network addresses are not allowed.");
                }
            }
        } catch (UnknownHostException unresolved) {
            throw new RemoteFetchException("Cannot resolve host: " + url.getHost());
        }
    }

    private String filenameFrom(URL url) {
        // url.getPath() is still percent-encoded — decode it before treating it as a filename, or a stray
        // "%" surviving into the stored name corrupts every Content-Disposition header and presigned link
        // built from it later.
        String path       = url.getPath();
        String rawName    = path.substring(path.lastIndexOf('/') + 1);
        int    queryIndex = rawName.indexOf('?');

        if (queryIndex > 0) {
            rawName = rawName.substring(0, queryIndex);
        }

        String  decodedName = UriUtils.decode(rawName, StandardCharsets.UTF_8);
        boolean unusable    = decodedName.isBlank() || !decodedName.contains(".");

        return unusable ? FALLBACK_FILENAME : decodedName;
    }
}
