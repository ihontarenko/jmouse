package org.jmouse.crawler.spi;

/**
 * Strategy interface for fetching remote resources. 📡
 *
 * <p>{@code Fetcher} abstracts the underlying transport mechanism
 * (HTTP client, mock fetcher, file-based fetcher, etc.) from the crawler runtime.</p>
 *
 * <p>Implementations are expected to be thread-safe unless explicitly documented otherwise.</p>
 *
 * <p>Error handling is delegated to implementations:
 * failures may be expressed via exceptions or encoded into {@link FetchResult}
 * depending on the strategy.</p>
 */
public interface Fetcher {

    /**
     * Fetch a remote resource described by the given {@link FetchRequest}.
     *
     * @param request fetch request description
     * @return fetch result containing response metadata and payload
     * @throws Exception if the fetch operation fails
     */
    FetchResult fetch(FetchRequest request) throws Exception;
}
