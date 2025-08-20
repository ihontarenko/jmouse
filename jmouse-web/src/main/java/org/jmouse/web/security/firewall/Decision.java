package org.jmouse.web.security.firewall;

import org.jmouse.web.http.HttpStatus;

import static org.jmouse.web.http.HttpStatus.OK;

public record Decision(Action action, HttpStatus httpStatus, String reason) {

    public boolean isNotAllowed() {
        return action != Action.ALLOW;
    }

    public boolean isBlocked() {
        return !isNotAllowed();
    }

    public static Decision allow() {
        return new Decision(Action.ALLOW, OK, OK.getText());
    }

    public static Decision block(HttpStatus status, String reason) {
        return new Decision(Action.BLOCK, status, reason);
    }

    public static Decision challenge(HttpStatus status, String reason) {
        return new Decision(Action.CHALLENGE, status, reason);
    }

    public static Decision block(int status, String reason) {
        return new Decision(Action.BLOCK, HttpStatus.ofCode(status), reason);
    }

    public static Decision challenge(int status, String reason) {
        return new Decision(Action.CHALLENGE, HttpStatus.ofCode(status), reason);
    }

    public enum Action {ALLOW, CHALLENGE, BLOCK}

}
