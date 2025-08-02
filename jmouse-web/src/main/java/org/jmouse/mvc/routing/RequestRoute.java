package org.jmouse.mvc.routing;

import org.jmouse.core.MediaType;
import org.jmouse.web.request.Headers;
import org.jmouse.web.request.http.HttpMethod;

import java.util.List;

/**
 * Represents incoming request for match evaluation.
 */
public record RequestRoute(String path, HttpMethod method, Headers headers, MediaType contentType, List<MediaType> accept) {
}