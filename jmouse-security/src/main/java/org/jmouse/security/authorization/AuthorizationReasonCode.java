package org.jmouse.security.authorization;

public enum AuthorizationReasonCode implements ReasonCode {

    ACCESS_DENIED(false),
    PERMITTED(true);

    private final boolean granted;

    AuthorizationReasonCode(boolean granted) {
        this.granted = granted;
    }

    @Override
    public boolean isGranted() {
        return granted;
    }

}
