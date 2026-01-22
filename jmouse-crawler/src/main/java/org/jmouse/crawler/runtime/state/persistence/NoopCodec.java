package org.jmouse.crawler.runtime.state.persistence;

public class NoopCodec implements Codec {

    @Override
    public <T> String encode(T value) {
        throw new UnsupportedOperationException("Encoding not supported");
    }

    @Override
    public <T> T decode(String encoded, Class<T> type) {
        throw new UnsupportedOperationException("Decoding not supported");
    }

}
