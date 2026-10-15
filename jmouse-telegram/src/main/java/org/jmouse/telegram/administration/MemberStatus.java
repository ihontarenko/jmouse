package org.jmouse.telegram.administration;

/**
 * Where somebody stands in a chat.
 */
public enum MemberStatus {

    CREATOR("creator"),
    ADMINISTRATOR("administrator"),
    MEMBER("member"),
    /** A member with fewer permissions than the chat's defaults, possibly until a date. */
    RESTRICTED("restricted"),
    LEFT("left"),
    /** Telegram's word is {@code kicked}; it means banned, possibly until a date. */
    BANNED("kicked");

    private final String wireName;

    MemberStatus(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    /**
     * @throws IllegalArgumentException for a status Telegram has added since — a sentence rather than a
     *                                  silent {@code MEMBER}, because guessing someone's standing in a
     *                                  chat is how a banned person gets treated as a member
     */
    public static MemberStatus fromWire(String wireName) {
        for (MemberStatus status : values()) {
            if (status.wireName.equals(wireName)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Telegram reported a member status this library does not know: " + wireName);
    }

    /** Whether they are in the chat at all. */
    public boolean present() {
        return this != LEFT && this != BANNED;
    }
}
