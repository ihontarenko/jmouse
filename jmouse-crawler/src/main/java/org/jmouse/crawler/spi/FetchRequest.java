package org.jmouse.crawler.spi;

import java.net.URI;
import java.util.Map;

/**
 * Immutable description of an HTTP fetch request. 🌐
 *
 * <p>{@code FetchRequest} encapsulates all information required by a
 * {@link Fetcher} to perform a single fetch operation.</p>
 *
 * <p>The record is intentionally minimal and protocol-agnostic:
 * it does not expose HTTP methods, bodies, or cookies.
 * This keeps the SPI small and easy to evolve.</p>
 *
 * <p>Headers provided here are applied in addition to any default headers
 * configured on the {@link Fetcher} implementation.</p>
 *
 * @param url     target URL to fetch (must be non-null)
 * @param headers request-specific headers (may be empty, but not {@code null})
 */
public record FetchRequest(URI url, Map<String, String> headers) { }
