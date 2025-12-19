package org.jmouse.jdbc.dialect;

public final class FixedDialectResolver implements DialectResolver {

    private final String dialectId;

    public FixedDialectResolver(String dialectId) {
        this.dialectId = dialectId;
    }

    @Override
    public String resolveDialectId() {
        return dialectId;
    }
}
