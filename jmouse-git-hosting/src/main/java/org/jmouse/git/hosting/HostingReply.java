package org.jmouse.git.hosting;

/**
 * An answer from a hosting, with the rate-limit state reported alongside it so a caller can pace itself.
 *
 * @param value     the answer
 * @param rateLimit the rate-limit state of the last request the answer took; {@link RateLimit#NOT_REPORTED} when
 *                  the hosting sent none
 * @param <T>       the kind of answer
 */
public record HostingReply<T>(T value, RateLimit rateLimit) {

    public HostingReply {
        if (value == null) {
            throw new IllegalArgumentException("A hosting reply needs a value.");
        }
        if (rateLimit == null) {
            rateLimit = RateLimit.NOT_REPORTED;
        }
    }
}
