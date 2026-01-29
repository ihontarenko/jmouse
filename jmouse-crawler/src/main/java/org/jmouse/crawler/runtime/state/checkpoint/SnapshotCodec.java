package org.jmouse.crawler.runtime.state.checkpoint;

public interface SnapshotCodec {
    <T> String encode(T value);
    <T> T decode(String raw, Class<T> type);
}
