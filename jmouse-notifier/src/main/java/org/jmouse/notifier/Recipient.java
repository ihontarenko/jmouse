package org.jmouse.notifier;

/**
 * Who a notification is for, in the product's own terms.
 *
 * <p>The library knows no people. Every case except {@link Direct} is handed to the product's
 * {@link RecipientResolver}, which turns it into {@link Addressee}s; each {@link Channel} then finds its
 * own addresses for them. So {@code Subject("42")} means whatever "42" means to the product, and a
 * scope is a word the product chose — a household, a workspace, an archive.
 */
public sealed interface Recipient {

    /**
     * Everybody within a scope.
     *
     * @param scope the product's word for "where" — {@code null} for the whole installation
     */
    record Everyone(String scope) implements Recipient {
    }

    /**
     * One person, an account, anything the product addresses by an identifier.
     *
     * @param id the product's identifier
     */
    record Subject(String id) implements Recipient {

        public Subject {
            requireText(id, "A subject recipient needs an identifier");
        }
    }

    /**
     * Everybody in a group the product keeps — a role, a team, a profile.
     *
     * @param id the product's identifier for the group
     */
    record Group(String id) implements Recipient {

        public Group {
            requireText(id, "A group recipient needs an identifier");
        }
    }

    /**
     * One device — a television, a browser.
     *
     * @param id the product's identifier for the device
     */
    record Device(String id) implements Recipient {

        public Device {
            requireText(id, "A device recipient needs an identifier");
        }
    }

    /**
     * An audience minus one device — "everyone but the screen where it just happened, which already
     * knows".
     *
     * <p>⚠️ A channel that does not address devices (Telegram reaches a person, not a television) still
     * reaches the person whose device was left out. Leaving a device out is about not repeating
     * something on the screen that showed it, not about keeping it from a person.
     *
     * @param audience who it is for
     * @param deviceId the device to leave out
     */
    record Except(Recipient audience, String deviceId) implements Recipient {

        public Except {
            if (audience == null) {
                throw new IllegalArgumentException("An exception needs the audience it narrows");
            }

            requireText(deviceId, "An exception needs the device to leave out");
        }
    }

    /**
     * One address on one channel, with no resolution at all — a group chat, an operator's channel.
     *
     * <p>⚠️ <strong>It bypasses the product's resolver, so who may publish one is the product's
     * question.</strong> A person allowed to choose a raw address can send anything anywhere the
     * channel's identity reaches.
     *
     * @param address where to deliver
     */
    record Direct(Address address) implements Recipient {

        public Direct {
            if (address == null) {
                throw new IllegalArgumentException("A direct recipient needs an address");
            }
        }
    }

    private static void requireText(String value, String complaint) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(complaint);
        }
    }
}
