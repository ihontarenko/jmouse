package org.jmouse.http;

public interface Header {

    String toHeaderValue();

    HttpHeader toHttpHeader();

}
