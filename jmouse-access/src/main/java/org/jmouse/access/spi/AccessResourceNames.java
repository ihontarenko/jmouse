package org.jmouse.access.spi;

import java.util.Optional;

/**
 * Reading {@link AccessResourceName} back — the one place a resource name is produced.
 *
 * <p>Deliberately without a fallback: a type with no annotation has no name, and asking for one is a
 * wiring fault reported at startup rather than a string invented on the spot. See the annotation for why
 * deriving it from the class name was the wrong answer.
 */
public final class AccessResourceNames {

    private AccessResourceNames() {
    }

    /** The word this type is written as, or a failure naming the class that needs the annotation. */
    public static String of(Class<?> type) {
        return find(type).orElseThrow(() -> new IllegalStateException(
                type.getName() + " takes part in access control but does not say what a policy calls it. "
                + "Add @AccessResourceName(\"" + suggestionFor(type) + "\") to the class — the word is "
                + "written down rather than derived so that renaming the class cannot silently change "
                + "what every rule spelling it means."));
    }

    /** The same, for a caller that has something to do when a type is not part of this at all. */
    public static Optional<String> find(Class<?> type) {
        return Optional.ofNullable(type.getAnnotation(AccessResourceName.class))
                .map(AccessResourceName::value);
    }

    /**
     * What the class would most likely be called — used only inside the failure above.
     *
     * <p>⚠️ A suggestion in a message, never a value. That distinction is the whole point of the
     * annotation: the moment this is allowed to answer for a missing one, the invisible convention is
     * back and the failure that would have taught somebody about it is gone.
     */
    private static String suggestionFor(Class<?> type) {
        return type.getSimpleName().replaceAll("(?<=[a-z0-9])(?=[A-Z])", "_").toLowerCase();
    }
}
