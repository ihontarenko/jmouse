package org.jmouse.notifier;

/**
 * One concrete party a {@link Recipient} resolved to — what a {@link Channel} finds addresses for.
 *
 * @param subject  the product's identifier for the person or account
 * @param deviceId the device this addressee was resolved through, or {@code null} when the product
 *                 addresses the subject as a whole. A channel that reaches people rather than devices
 *                 ignores it
 */
public record Addressee(String subject, String deviceId) {

    public Addressee {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("An addressee needs a subject");
        }
    }

    public static Addressee of(String subject) {
        return new Addressee(subject, null);
    }

    public static Addressee onDevice(String subject, String deviceId) {
        return new Addressee(subject, deviceId);
    }
}
