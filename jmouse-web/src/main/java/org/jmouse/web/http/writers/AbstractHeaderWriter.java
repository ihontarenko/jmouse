package org.jmouse.web.http.writers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.http.Header;
import org.jmouse.web.http.HeaderWriter;

public class AbstractHeaderWriter implements HeaderWriter {

    private final Header header;

    public AbstractHeaderWriter(Header header) {
        this.header = header;
    }

    @Override
    public void writeHeaders(HttpServletRequest request, HttpServletResponse response) {
        writeHeader(response, header.toHttpHeader(), header.toHeaderValue());
    }

}
