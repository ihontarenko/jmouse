package svit.io;

import svit.matcher.Matcher;

import java.util.Collection;
import java.util.List;

import static svit.reflection.Reflections.getShortName;

public interface ResourceLoader {

    String UNSUPPORTED_EXCEPTION = "Unsupported for '%s' implementation.";

    default Collection<Resource> loadResources(String location) {
        return loadResources(location, Matcher.constant(true));
    }

    default Collection<Resource> loadResources(String location, Matcher<Resource> matcher) {
        throw new UnsupportedOperationException(UNSUPPORTED_EXCEPTION.formatted(getShortName(this)));
    }

    default Resource getResource(String location) {
        throw new UnsupportedOperationException(UNSUPPORTED_EXCEPTION.formatted(getShortName(this)));
    }

    default ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    default boolean supports(String protocol) {
        return supportedProtocols().contains(protocol);
    }

    List<String> supportedProtocols();

}
