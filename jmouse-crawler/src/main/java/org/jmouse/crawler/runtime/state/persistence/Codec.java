package org.jmouse.crawler.runtime.state.persistence;

public interface Codec {

    <T> String encode(T value);

    <T> T decode(String encoded, Class<T> type);

}
