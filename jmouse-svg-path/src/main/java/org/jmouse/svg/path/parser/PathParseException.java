package org.jmouse.svg.path.parser;

import org.jmouse.el.parser.ParseException;

/** Path data that is not SVG path data: what is wrong, and at which offset of the text it stands. */
public final class PathParseException extends ParseException {

    private final int    offset;
    private final String detail;

    public PathParseException(String name, int offset, String detail) {
        super(name + ", at " + offset + ": " + detail);
        this.offset = offset;
        this.detail = detail;
    }

    /** Where in the path data it stands, from 0. */
    public int offset() {
        return offset;
    }

    /** What is wrong, without where. */
    public String detail() {
        return detail;
    }
}
