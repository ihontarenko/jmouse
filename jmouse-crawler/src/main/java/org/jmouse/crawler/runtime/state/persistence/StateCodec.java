package org.jmouse.crawler.runtime.state.persistence;

public interface StateCodec {

    String encode(Object dto);

    <T> T decode(String line, Class<T> type);

}