package org.jmouse.common.ast.recognizer;

import org.jmouse.common.ast.Priority;

import java.util.Optional;

public interface Recognizer<R, T> extends Priority {
    Optional<R> recognize(T subject);
}
