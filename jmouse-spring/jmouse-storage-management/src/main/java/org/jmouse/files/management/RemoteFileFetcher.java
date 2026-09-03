package org.jmouse.files.management;

import org.jmouse.files.OwnerReference;
import org.jmouse.files.exception.RemoteFetchException;
import org.jmouse.storage.policy.FixedUploadPolicy;
import org.jmouse.storage.policy.UploadPolicy;
import org.jmouse.storage.policy.UploadPolicyResolver;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/**
 * 🌐 Downloads a file from a web address on somebody's behalf.
 *
 * <p>Owns every network-side concern of importing: the SSRF guard against internal addresses,
 * redirects, timeouts, response sanity checks, filename derivation, and the early size refusal based on
 * the declared {@code Content-Length}. What happens to the bytes afterwards is
 * {@link FileManagement}'s.</p>
 *
 * <h3>⚠️ An import is judged by the same acceptance policy as an upload</h3>
 *
 * <p>It is the one place the content is chosen by somebody who is not the caller, which makes it the
 * <em>more</em> dangerous of the two rather than the less. This refuses an oversized declaration up
 * front; the real size and type are judged by the ingestion path once the bytes arrive, exactly as they
 * are for a multipart upload.</p>
 *
 * <h3>⚠️ The SSRF guard is the load-bearing part, and it runs on every hop</h3>
 *
 * <p>Without it, "import this URL" is a request that makes <strong>this server</strong> fetch things on
 * the caller's behalf — cloud metadata endpoints, internal admin pages, other services on the same
 * network — and hand the result back. Loopback, site-local, link-local and wildcard addresses are all
 * refused, for every address the host resolves to rather than the first.</p>
 *
 * <p>Which is the second reason redirects are followed by hand rather than by
 * {@link HttpURLConnection#setInstanceFollowRedirects(boolean)}: the built-in follower checks nothing,
 * so a public address answering {@code 302 Location: http://169.254.169.254/…} walked straight past a
 * guard that had already run. Every hop here is re-checked, and the scheme allow-list is re-applied
 * with it.</p>
 *
 * <h3>⚠️ Why it announces itself as a browser</h3>
 *
 * <p>The addresses this actually fetches are distributor photographs and manufacturer datasheets, and
 * both are routinely behind hotlink protection or a bot filter. Those answer an unrecognised client
 * with {@code 200} and a web page rather than an error, so the failure arrives looking like "the file
 * is an HTML document" — while the very same URL renders perfectly in the tab the user is looking at.
 * A named-tool {@code User-Agent} is the one thing separating the two, so the default is a browser's,
 * and a {@code Referer} from the address's own origin goes with it because that is what hotlink
 * protection is checking for. Both are overridable; see the {@code user-agent} property.</p>
 *
 * <h3>⚠️ A refusal names the address and what came back</h3>
 *
 * <p>Every message here carries the URL, and the HTML refusal carries the content type as well. These
 * sentences are shown to whoever pressed the button — the caller has one address that failed among ten
 * that worked, and "the download failed" does not tell them which.</p>
 */
public class RemoteFileFetcher {

    private static final int CONNECT_TIMEOUT_MILLISECONDS = 10_000;
    private static final int READ_TIMEOUT_MILLISECONDS    = 30_000;
    private static final int MAXIMUM_REDIRECTS            = 5;

    private static final String ACCEPTED_CONTENT_TYPES = "application/pdf,image/*,application/octet-stream,*/*";
    private static final String ACCEPTED_LANGUAGES     = "en-US,en;q=0.9";

    /** ⚠️ Anything else — {@code file:}, {@code jar:}, {@code ftp:} — reads bytes the caller never named. */
    private static final Set<String> DOWNLOADABLE_SCHEMES = Set.of("http", "https");

    /** Used when the URL path carries no usable filename; datasheets are the dominant use case. */
    private static final String FALLBACK_FILENAME = "datasheet.pdf";

    private final UploadPolicyResolver uploadPolicies;
    private final String               userAgent;

    /**
     * 🏗️ Build a fetcher that announces itself as this installation.
     *
     * <p>⚠️ <strong>A resolver rather than a policy, and that is not tidiness.</strong> This is the
     * second write path into storage, and it is the one with a size limit of its own — so a fetcher
     * holding a single policy is a fetcher that walks straight past whatever a destination says it
     * accepts, while every multipart upload obeys it.</p>
     *
     * @param uploadPolicies what this installation accepts, per destination
     * @param userAgent      what to send as {@code User-Agent} — most media and datasheet hosts refuse a
     *                       client they do not recognise as a browser
     */
    public RemoteFileFetcher(UploadPolicyResolver uploadPolicies, String userAgent) {
        this.uploadPolicies = uploadPolicies;
        this.userAgent      = userAgent;
    }

    /**
     * 🏗️ Build a fetcher over one policy that applies everywhere.
     *
     * @param uploadPolicy what this installation accepts
     * @param userAgent    what to announce this installation as
     * @deprecated pass a {@link UploadPolicyResolver} instead, so a fetch obeys the rule of wherever
     *             the file is headed.
     */
    @Deprecated(since = "1.1")
    public RemoteFileFetcher(UploadPolicy uploadPolicy, String userAgent) {
        this(new FixedUploadPolicy(uploadPolicy), userAgent);
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
     * 📥 Open {@code sourceUrl} and hand back its name and content stream, judged by the installation's
     * own rule.
     *
     * @param sourceUrl the address to fetch
     * @return the open download
     * @throws RemoteFetchException when the URL is unusable, resolves to an internal address, redirects
     *                              endlessly, answers with an error status or an HTML page, declares an
     *                              oversized {@code Content-Length}, or fails on the network
     */
    public RemoteFile fetch(String sourceUrl) {
        return fetch(sourceUrl, null);
    }

    /**
     * 📥 Open {@code sourceUrl} and hand back its name and content stream, judged by the rule of
     * wherever the file is headed.
     *
     * @param sourceUrl the address to fetch
     * @param owner     what will hold the file, or {@code null} to apply the installation's own rule
     * @return the open download
     * @throws RemoteFetchException when the URL is unusable, resolves to an internal address, redirects
     *                              endlessly, answers with an error status or an HTML page, declares an
     *                              oversized {@code Content-Length}, or fails on the network
     */
    public RemoteFile fetch(String sourceUrl, OwnerReference owner) {
        UploadPolicy uploadPolicy = owner == null
                ? uploadPolicies.policyFor(null, null)
                : uploadPolicies.policyFor(owner.ownerType(), owner.ownerId());

        try {
            HttpURLConnection connection = follow(addressOf(sourceUrl));

            requireNonHtmlContent(connection);

            // Refuse oversized downloads before spending the bandwidth, when the server admits the size.
            // Servers that omit or understate Content-Length are caught once the real size is known.
            uploadPolicy.ensureWithinSizeLimit(connection.getContentLengthLong());

            // ⚠️ The name comes from where the download ENDED, not where it started. A datasheet link is
            // regularly a redirect off a tracking address with no filename in it at all.
            return new RemoteFile(filenameFrom(connection.getURL()), connection.getInputStream());
        } catch (RemoteFetchException refusal) {
            throw refusal;
        } catch (IOException | IllegalArgumentException failure) {
            throw new RemoteFetchException(
                    "Failed to download " + sourceUrl + " — " + failure.getMessage());
        }
    }

    /**
     * The address to start from, checked before a single packet is sent.
     *
     * <p>⚠️ A <strong>protocol-relative</strong> address ({@code //host/path}) is normalised rather than
     * refused. It is what a page embeds to inherit its own scheme, so it is a shape distributor APIs
     * hand out freely — and there is no page here to inherit from, which made it fail as the
     * unactionable <em>no protocol</em>.</p>
     */
    private URI addressOf(String sourceUrl) {
        String trimmed = sourceUrl == null ? "" : sourceUrl.trim();

        if (trimmed.isEmpty()) {
            throw new RemoteFetchException("No address was given to download from.");
        }

        URI address = URI.create(trimmed.startsWith("//") ? "https:" + trimmed : trimmed);

        requireDownloadableAddress(address);

        return address;
    }

    /**
     * 🚦 Walk the redirect chain by hand, checking every hop.
     *
     * @return the connection that answered with content
     */
    private HttpURLConnection follow(URI address) throws IOException {
        URI current = address;

        for (int hop = 0; hop <= MAXIMUM_REDIRECTS; hop++) {
            HttpURLConnection connection = open(current);
            int               status     = connection.getResponseCode();

            if (!isRedirect(status)) {
                requireSuccessStatus(status, current);

                return connection;
            }

            URI next = redirectTargetOf(connection, status, current);

            connection.disconnect();
            requireDownloadableAddress(next);

            current = next;
        }

        throw new RemoteFetchException(
                "Gave up on " + address + " after " + MAXIMUM_REDIRECTS + " redirects.");
    }

    private HttpURLConnection open(URI address) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) address.toURL().openConnection();

        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLISECONDS);
        connection.setReadTimeout(READ_TIMEOUT_MILLISECONDS);
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setRequestProperty("Accept", ACCEPTED_CONTENT_TYPES);
        connection.setRequestProperty("Accept-Language", ACCEPTED_LANGUAGES);

        // Hotlink protection is the ordinary reason a picture a browser renders cannot be fetched by a
        // server: the CDN wants a Referer belonging to the site the image is part of. The address's own
        // origin is the one referer that is simply true — the file really is being asked for from there.
        connection.setRequestProperty("Referer", originOf(address));

        // ⚠️ Followed by hand instead — see the class note. The built-in follower re-checks nothing and
        // silently refuses to cross http↔https, which surfaces as an unexplained 30x rather than a hop.
        connection.setInstanceFollowRedirects(false);
        connection.connect();

        return connection;
    }

    private boolean isRedirect(int status) {
        return status == HttpURLConnection.HTTP_MOVED_PERM
               || status == HttpURLConnection.HTTP_MOVED_TEMP
               || status == HttpURLConnection.HTTP_SEE_OTHER
               || status == 307
               || status == 308;
    }

    /** Where a redirect points, resolved against the address it came from so a relative one works. */
    private URI redirectTargetOf(HttpURLConnection connection, int status, URI from) {
        String location = connection.getHeaderField("Location");

        if (location == null || location.isBlank()) {
            throw new RemoteFetchException(
                    from + " answered HTTP " + status + " without saying where to go next.");
        }

        try {
            return from.resolve(location.trim());
        } catch (IllegalArgumentException unusable) {
            throw new RemoteFetchException(
                    from + " redirected to an address that cannot be read: " + location);
        }
    }

    private void requireSuccessStatus(int status, URI address) {
        if (status < 200 || status >= 300) {
            throw new RemoteFetchException("Remote server answered HTTP " + status + " for " + address);
        }
    }

    private void requireNonHtmlContent(HttpURLConnection connection) {
        String contentType = connection.getContentType();

        if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("text/html")) {
            throw new RemoteFetchException(
                    connection.getURL() + " returned a web page (" + contentType + ") instead of a file"
                    + " — it needs a sign-in, or the server refused an automated request.");
        }
    }

    /** ⚠️ Both halves of "may this be fetched at all": the scheme, and where the host actually points. */
    private void requireDownloadableAddress(URI address) {
        requireDownloadableScheme(address);
        requireExternalAddress(address);
    }

    private void requireDownloadableScheme(URI address) {
        String scheme = address.getScheme();

        if (scheme == null || !DOWNLOADABLE_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
            throw new RemoteFetchException(
                    "Only http and https addresses can be imported, and this one is not: " + address);
        }
    }

    /** ⚠️ Guard against SSRF: refuse URLs whose host resolves to a loopback, private or link-local address. */
    private void requireExternalAddress(URI address) {
        String host = address.getHost();

        if (host == null || host.isBlank()) {
            throw new RemoteFetchException("Address names no host to download from: " + address);
        }

        try {
            for (InetAddress resolved : InetAddress.getAllByName(host)) {
                if (resolved.isLoopbackAddress() || resolved.isSiteLocalAddress()
                        || resolved.isLinkLocalAddress() || resolved.isAnyLocalAddress()) {
                    throw new RemoteFetchException(
                            "Requests to internal or reserved network addresses are not allowed: " + host);
                }
            }
        } catch (UnknownHostException unresolved) {
            throw new RemoteFetchException("Cannot resolve host: " + host);
        }
    }

    private String originOf(URI address) {
        return address.getScheme() + "://" + address.getAuthority() + "/";
    }

    private String filenameFrom(URL url) {
        // url.getPath() is still percent-encoded — decode it before treating it as a filename, or a stray
        // "%" surviving into the stored name corrupts every Content-Disposition header and presigned link
        // built from it later.
        String path    = url.getPath();
        String rawName = path.substring(path.lastIndexOf('/') + 1);

        String  decodedName = UriUtils.decode(rawName, StandardCharsets.UTF_8);
        boolean unusable    = decodedName.isBlank() || !decodedName.contains(".");

        return unusable ? FALLBACK_FILENAME : decodedName;
    }
}
