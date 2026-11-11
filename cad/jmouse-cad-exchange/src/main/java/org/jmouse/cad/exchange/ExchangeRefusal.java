package org.jmouse.cad.exchange;

/** A file of a format that does not read: what is wrong, and where. */
public class ExchangeRefusal extends RuntimeException {

    private final String detail;
    private final int    line;
    private final int    column;

    public ExchangeRefusal(String detail, int line, int column) {
        super(detail + " (line " + line + ", column " + column + ")");
        this.detail = detail;
        this.line = line;
        this.column = column;
    }

    public String detail() {
        return detail;
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }
}
