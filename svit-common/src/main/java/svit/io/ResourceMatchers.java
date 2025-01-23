package svit.io;

import svit.matcher.Matcher;

/**
 * Matchers for {@link Resource} objects.
 */
final public class ResourceMatchers {

    /**
     * Returns a matcher that checks if the resource's name matches a given regex pattern.
     *
     * @param regex the regex pattern.
     * @return a matcher that returns true if the resource's name matches the regex.
     */
    public static Matcher<Resource> nameMatches(String regex) {
        return resource -> resource.getName().matches(regex);
    }

    /**
     * Returns a matcher that checks if the resource exists.
     *
     * @return a matcher that returns true if the resource exists.
     */
    public static Matcher<Resource> exists() {
        return Resource::exists;
    }

    /**
     * Returns a matcher that checks if the resource is readable.
     *
     * @return a matcher that returns true if the resource is readable.
     */
    public static Matcher<Resource> isReadable() {
        return ReadableResource::isReadable;
    }

}
