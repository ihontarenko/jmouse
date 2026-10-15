package org.jmouse.notifier;

/**
 * Where one channel delivers — a chat, a device, a mailbox.
 *
 * @param channel the {@link Channel#name()} this address belongs to
 * @param value   the channel's own notation; opaque to everything else
 */
public record Address(String channel, String value) {

    public Address {
        if (channel == null || channel.isBlank()) {
            throw new IllegalArgumentException("An address needs the channel it belongs to");
        }

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("An address needs a value");
        }
    }

    @Override
    public String toString() {
        return channel + ":" + value;
    }
}
