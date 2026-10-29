package org.jmouse.gerber.node;

import java.util.Arrays;
import java.util.Optional;

/** What an attribute is attached to, as its command says it. */
public enum AttributeScope {

    /** {@code TF}: the file — its function, its polarity, the software that wrote it. */
    FILE("TF"),
    /** {@code TA}: every aperture defined after it — what a pad or a track is for. */
    APERTURE("TA"),
    /** {@code TO}: every object drawn after it — its net, its component. */
    OBJECT("TO"),
    /** {@code TD}: deletes an aperture or an object attribute, or all of them. */
    DELETE("TD");

    private final String command;

    AttributeScope(String command) {
        this.command = command;
    }

    public String command() {
        return command;
    }

    /** The scope an extended body opens with: {@code TF.FileFunction,…} is a file's. */
    public static Optional<AttributeScope> ofBody(String body) {
        return Arrays.stream(values()).filter(scope -> body.startsWith(scope.command)).findFirst();
    }
}
