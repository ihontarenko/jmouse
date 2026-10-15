package org.jmouse.telegram.administration;

import java.util.EnumSet;
import java.util.Set;

/**
 * One thing an ordinary member may do in a chat, by Telegram's own name for it.
 *
 * <p>⚠️ Sent as a <em>complete</em> set every time: a permission absent from the set is sent as
 * {@code false}, never left out. Telegram treats an omitted field differently in different methods, and
 * "I meant unchanged" versus "I meant no" is not a guess this library makes on anybody's behalf.
 */
public enum ChatPermission {

    SEND_MESSAGES("can_send_messages"),
    SEND_AUDIOS("can_send_audios"),
    SEND_DOCUMENTS("can_send_documents"),
    SEND_PHOTOS("can_send_photos"),
    SEND_VIDEOS("can_send_videos"),
    SEND_VIDEO_NOTES("can_send_video_notes"),
    SEND_VOICE_NOTES("can_send_voice_notes"),
    SEND_POLLS("can_send_polls"),
    SEND_OTHER_MESSAGES("can_send_other_messages"),
    ADD_WEB_PAGE_PREVIEWS("can_add_web_page_previews"),
    CHANGE_INFO("can_change_info"),
    INVITE_USERS("can_invite_users"),
    PIN_MESSAGES("can_pin_messages"),
    MANAGE_TOPICS("can_manage_topics");

    private final String wireName;

    ChatPermission(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    /** Everything — what a member of an unrestricted chat may do. */
    public static Set<ChatPermission> all() {
        return EnumSet.allOf(ChatPermission.class);
    }

    /** Nothing — a read-only member. */
    public static Set<ChatPermission> none() {
        return EnumSet.noneOf(ChatPermission.class);
    }
}
