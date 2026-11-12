package org.jmouse.http.client;

import org.jmouse.http.HttpMethod;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A call out, whole: its method and address, its headers, its body, how long it may take from sending to the last byte
 * of the answer, and how much of an answer it may read. Made with {@link #to}; a call never follows a redirect on its
 * own — a caller that wants one says so to its transport.
 *
 * @param headers       each header's values, by its name as written
 * @param timeout       how long the whole call may take, the answer's body read included
 * @param responseLimit how many bytes of an answer's body it reads at most; past them the call fails
 */
public record HttpCall(HttpMethod method, URI uri, Map<String, List<String>> headers, Optional<Body> body, Duration timeout,
                       long responseLimit) {

    /** What a call goes over. */
    static final Set<String> SCHEMES = Set.of("http", "https");

    /** How long a call may take when its caller does not say. */
    public static final Duration USUAL_TIMEOUT = Duration.ofSeconds(30);

    /** How much of an answer a call reads when its caller does not say: 16 MiB. */
    public static final long USUAL_RESPONSE_LIMIT = 16L * 1024 * 1024;

    public HttpCall {
        if (method == null || uri == null) {
            throw new IllegalArgumentException("A call has a method and an address.");
        }
        if (!uri.isAbsolute() || uri.getHost() == null || !SCHEMES.contains(uri.getScheme().toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("A call goes to an absolute HTTP or HTTPS address with a host.");
        }
        if (uri.getRawUserInfo() != null) {
            // A credential in the address rides along unseen, into logs and redirects: it goes in a header or nowhere.
            throw new IllegalArgumentException("A call's address carries no credentials.");
        }
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("A call takes a positive time, not " + timeout + ".");
        }
        if (responseLimit <= 0) {
            throw new IllegalArgumentException("A call reads a positive number of bytes, not " + responseLimit + ".");
        }
        Map<String, List<String>> copied = new LinkedHashMap<>();
        headers.forEach((name, values) -> {
            requireHeader(name, values);
            copied.put(name, List.copyOf(values));
        });
        headers = Collections.unmodifiableMap(copied);
        body = body == null ? Optional.empty() : body;
    }

    /** The characters a header's name is written in: a token of RFC 9110. */
    private static final String TOKEN = "!#$%&'*+-.^_`|~0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /**
     * A header a call may carry: its name a token, no value breaking a line — which would smuggle a header of its own.
     * The refusal names the header, never its value: that may be a credential.
     */
    private static void requireHeader(String name, List<String> values) {
        if (name == null || name.isEmpty() || !name.chars().allMatch(character -> TOKEN.indexOf(character) >= 0)) {
            throw new IllegalArgumentException("A header's name is a token of letters, digits and !#$%&'*+-.^_`|~.");
        }
        for (String value : values) {
            if (value == null || value.chars().anyMatch(character -> character == '\r' || character == '\n' || character == 0)) {
                throw new IllegalArgumentException("The header " + name + " has a value that breaks a line.");
            }
        }
    }

    /** A body sent with a call: its bytes and what they are. */
    public record Body(byte[] bytes, String contentType) {

        public Body {
            bytes = bytes.clone();
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
        }
    }

    /** A call of this method to this address, taking the usual time and reading the usual answer. */
    public static Builder to(HttpMethod method, URI uri) {
        return new Builder(method, uri);
    }

    public static Builder get(URI uri) {
        return to(HttpMethod.GET, uri);
    }

    public static Builder post(URI uri) {
        return to(HttpMethod.POST, uri);
    }

    /** A call made a step at a time. */
    public static final class Builder {

        private final HttpMethod method;
        private final URI uri;
        private final Map<String, List<String>> headers = new LinkedHashMap<>();
        private Body body;
        private Duration timeout = USUAL_TIMEOUT;
        private long responseLimit = USUAL_RESPONSE_LIMIT;

        private Builder(HttpMethod method, URI uri) {
            this.method = method;
            this.uri = uri;
        }

        /** A header's value, beside any it has already. */
        public Builder header(String name, String value) {
            headers.computeIfAbsent(name, ignored -> new ArrayList<>()).add(value);
            return this;
        }

        public Builder headers(Map<String, String> values) {
            values.forEach(this::header);
            return this;
        }

        /** Its body: these bytes, of this type. */
        public Builder body(byte[] bytes, String contentType) {
            this.body = new Body(bytes, contentType);
            return this;
        }

        /** Its body: this text in UTF-8, of this type. */
        public Builder text(String text, String contentType) {
            return body(text.getBytes(StandardCharsets.UTF_8), contentType);
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder responseLimit(long bytes) {
            this.responseLimit = bytes;
            return this;
        }

        public HttpCall build() {
            return new HttpCall(method, uri, headers, Optional.ofNullable(body), timeout, responseLimit);
        }
    }
}
