package org.jmouse.http;

abstract public class AbstractHeader implements Header {

    private final HttpHeader httpHeader;

    protected AbstractHeader(HttpHeader httpHeader) {
        this.httpHeader = httpHeader;
    }

    @Override
    public HttpHeader toHttpHeader() {
        return httpHeader;
    }

}
