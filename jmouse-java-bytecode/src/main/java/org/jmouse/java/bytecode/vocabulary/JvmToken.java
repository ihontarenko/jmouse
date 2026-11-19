package org.jmouse.java.bytecode.vocabulary;

public interface JvmToken {
    String name();

    String namespace();

    default int minimum() {
        return JvmVocabulary.range(this).minimum();
    }

    default int maximum() {
        return JvmVocabulary.range(this).maximum();
    }

    default int code() {
        JvmTokenRange range = JvmVocabulary.range(this);
        if (range.minimum() != range.maximum()) {
            throw new IllegalStateException("This token represents a range");
        }
        return range.minimum();
    }
}
