package org.jmouse.ai.management;

/**
 * Somebody asked this screen about an action that is not published.
 *
 * <p>Thrown rather than turned into a response body, because the shape of that body is the product's
 * decision and not this module's. A library that answered with its own error document would put a second
 * error format into an application that already has one, and the first person to notice would be a
 * client author reconciling them.
 *
 * <p>Its own type rather than a stock one so that a product's exception handler can map it precisely —
 * a {@code NoSuchElementException} escaping a controller could have come from anywhere.
 */
public class UnknownPublishedToolException extends RuntimeException {

    private final String publishedName;

    public UnknownPublishedToolException(String publishedName) {
        super("There is no action published as '" + publishedName + "'.");
        this.publishedName = publishedName;
    }

    public String publishedName() {
        return publishedName;
    }
}
