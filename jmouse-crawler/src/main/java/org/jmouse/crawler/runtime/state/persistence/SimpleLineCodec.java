package org.jmouse.crawler.runtime.state.persistence;

import java.util.Objects;

/**
 * Minimal line-based codec placeholder.
 *
 * <p>Recommended: replace with Jackson-based JSON codec in the next iteration.</p>
 */
public final class SimpleLineCodec implements StateCodec {

    @Override
    public String encode(Object dto) {
        return Objects.toString(dto);
    }

    @Override
    public <T> T decode(String line, Class<T> type) {
        throw new UnsupportedOperationException(
                "No decode implementation. Plug JacksonJsonCodec here."
        );
    }

    public static StateCodec noop() {
        return new SimpleLineCodec();
    }
}
