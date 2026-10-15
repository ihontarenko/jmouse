package org.jmouse.notifier;

/**
 * How much a notification matters.
 *
 * <p>⚠️ <strong>Sound is carried by the level, not chosen per call.</strong> A boolean beside every
 * publish would put the decision <em>should this interrupt somebody</em> somewhere other than the word
 * describing how bad it is. A channel asks {@link #audible()}; a reader who disagrees silences the
 * channel on their own device.
 */
public enum Level {

    /** Worth knowing. Silent. */
    INFO,

    /** Needs attention soon. Audible. */
    WARN,

    /** Something is wrong now. Audible. */
    ALARM;

    /** Whether a channel that can choose should make a sound. */
    public boolean audible() {
        return this != INFO;
    }
}
