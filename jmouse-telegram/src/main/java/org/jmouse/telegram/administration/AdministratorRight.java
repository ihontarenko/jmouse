package org.jmouse.telegram.administration;

/**
 * One power an administrator of a chat may hold, by Telegram's own name for it.
 *
 * <p>A set of these rather than fifteen booleans: a grant reads as the powers it names, and a power
 * Telegram adds tomorrow is one constant here rather than a sixteenth constructor argument everywhere.
 */
public enum AdministratorRight {

    MANAGE_CHAT("can_manage_chat"),
    DELETE_MESSAGES("can_delete_messages"),
    MANAGE_VIDEO_CHATS("can_manage_video_chats"),
    RESTRICT_MEMBERS("can_restrict_members"),
    PROMOTE_MEMBERS("can_promote_members"),
    CHANGE_INFO("can_change_info"),
    INVITE_USERS("can_invite_users"),
    POST_STORIES("can_post_stories"),
    EDIT_STORIES("can_edit_stories"),
    DELETE_STORIES("can_delete_stories"),
    /** Channels only. */
    POST_MESSAGES("can_post_messages"),
    /** Channels only. */
    EDIT_MESSAGES("can_edit_messages"),
    PIN_MESSAGES("can_pin_messages"),
    /** Forum supergroups only. */
    MANAGE_TOPICS("can_manage_topics");

    private final String wireName;

    AdministratorRight(String wireName) {
        this.wireName = wireName;
    }

    /** Telegram's field name for it. */
    public String wireName() {
        return wireName;
    }
}
