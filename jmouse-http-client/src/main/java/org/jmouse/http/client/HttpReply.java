package org.jmouse.http.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * An answer to a call: where it came from, its status, its headers, and its body as bytes — all of it, read within the
 * call's limit. A status that is no success is an answer like any other; what it means is the caller's to say.
 *
 * @param uri     the address that answered: the call's, or where a followed redirect led
 * @param headers each header's values, by its name in lower case
 */
public record HttpReply(URI uri, int status, Map<String, List<String>> headers, byte[] body) {

    public HttpReply {
        Map<String, List<String>> lowered = new LinkedHashMap<>();
        headers.forEach((name, values) -> lowered.merge(name.toLowerCase(Locale.ROOT), List.copyOf(values),
                (one, other) -> Stream.concat(one.stream(), other.stream()).toList()));
        headers = Collections.unmodifiableMap(lowered);
        body = body.clone();
    }

    @Override
    public byte[] body() {
        return body.clone();
    }

    /** Whether it says the call succeeded: a status of 2xx. */
    public boolean successful() {
        return status >= 200 && status < 300;
    }

    /** Its body read as UTF-8 text. */
    public String text() {
        return new String(body, StandardCharsets.UTF_8);
    }

    /** A header's first value, by its name in any case. */
    public Optional<String> header(String name) {
        return Optional.ofNullable(headers.get(name.toLowerCase(Locale.ROOT))).filter(values -> !values.isEmpty())
                .map(List::getFirst);
    }
}
