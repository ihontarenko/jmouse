package org.jmouse.java.bytecode.vocabulary;

public enum JvmStackMapFrameKind implements JvmToken {
    SAME_FRAME,
    SAME_LOCALS_ONE_STACK_ITEM_FRAME,
    RESERVED,
    SAME_LOCALS_ONE_STACK_ITEM_FRAME_EXTENDED,
    CHOP_FRAME,
    SAME_FRAME_EXTENDED,
    APPEND_FRAME,
    FULL_FRAME;

    @Override
    public String namespace() {
        return JvmVocabulary.namespace(JvmStackMapFrameKind.class);
    }

    public static JvmStackMapFrameKind of(int code) {
        return JvmVocabulary.token(JvmStackMapFrameKind.class, code);
    }
}
