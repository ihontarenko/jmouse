package svit.ast.recognizer;

import svit.ast.Priority;

import java.util.Optional;

public interface Recognizer<R, T> extends Priority {
    Optional<R> recognize(T subject);
}
