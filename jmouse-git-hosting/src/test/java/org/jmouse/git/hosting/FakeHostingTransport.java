package org.jmouse.git.hosting;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Canned answers by request path, and a record of every request made; no network. */
final class FakeHostingTransport implements HostingTransport {

    /** One request as the hosting sent it. */
    record Request(URI endpoint, Map<String, String> headers) {
    }

    private final ObjectMapper                     json      = new ObjectMapper();
    private final Map<String, HostingHttpResponse> responses = new HashMap<>();
    private final List<Request>                    requests  = new ArrayList<>();

    /** Answers a GET to this raw path (query excluded) with a success carrying a canned resource. */
    FakeHostingTransport answer(String rawPath, String resource, Map<String, String> headers) {
        return answer(rawPath, 200, headers, read(resource));
    }

    /** Answers a GET to this raw path with a status and no body. */
    FakeHostingTransport refuse(String rawPath, int status, Map<String, String> headers) {
        return answer(rawPath, status, headers, null);
    }

    private FakeHostingTransport answer(String rawPath, int status, Map<String, String> headers, Object body) {
        Map<String, List<String>> values = new LinkedHashMap<>();
        headers.forEach((name, value) -> values.put(name, List.of(value)));
        responses.put(rawPath, new HostingHttpResponse(status, HttpHeaders.of(values, (name, value) -> true), body));
        return this;
    }

    @Override
    public HostingHttpResponse get(URI endpoint, Map<String, String> headers, HostingLimits limits) {
        requests.add(new Request(endpoint, Map.copyOf(headers)));
        HostingHttpResponse response = responses.get(endpoint.getRawPath());
        if (response == null) {
            throw new AssertionError("Unexpected request to " + endpoint);
        }
        return response;
    }

    List<Request> requests() {
        return requests;
    }

    Request lastRequest() {
        return requests.getLast();
    }

    private Object read(String resource) {
        try (InputStream stream = FakeHostingTransport.class.getResourceAsStream("/" + resource)) {
            if (stream == null) {
                throw new AssertionError("Missing test resource " + resource);
            }
            return json.readValue(stream, Object.class);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}
