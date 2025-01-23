package svit.support.provider.error;

import svit.support.context.ErrorDetails;

import java.util.Map;
import java.util.Set;

public interface ErrorProvider<K> {

    ErrorDetails getError(K key);

    boolean hasError(K key);

    Map<K, ErrorDetails> getErrorsMap();

    Set<ErrorDetails> getErrorsSet();

}
