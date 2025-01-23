package svit.matcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * A utility class that provides matchers for working with {@link Path} objects.
 * These matchers can check various conditions like file existence, directory checks,
 * file permissions, file size, and more.
 */
public class PathMatchers {

    /**
     * Returns a matcher that checks if the file or directory at the given path exists.
     *
     * @return a matcher that returns true if the file or directory exists
     */
    public static Matcher<Path> exists() {
        return new PathExistsMatcher();
    }

    /**
     * Returns a matcher that checks if the path is a directory.
     *
     * @return a matcher that returns true if the path is a directory
     */
    public static Matcher<Path> isDirectory() {
        return new PathIsDirectoryMatcher();
    }

    /**
     * Returns a matcher that checks if the path is a regular file.
     *
     * @return a matcher that returns true if the path is a regular file
     */
    public static Matcher<Path> isFile() {
        return new PathIsFileMatcher();
    }

    /**
     * Returns a matcher that checks if the file has the specified extension.
     *
     * @param extension the file extension to check for
     * @return a matcher that returns true if the file has the given extension
     */
    public static Matcher<Path> hasExtension(String extension) {
        return new PathHasExtensionMatcher(extension);
    }

    /**
     * Returns a matcher that checks if the path is readable.
     *
     * @return a matcher that returns true if the path is readable
     */
    public static Matcher<Path> isReadable() {
        return new PathIsReadableMatcher();
    }

    /**
     * Returns a matcher that checks if the path is writable.
     *
     * @return a matcher that returns true if the path is writable
     */
    public static Matcher<Path> isWritable() {
        return new PathIsWritableMatcher();
    }

    /**
     * Returns a matcher that checks if the file size matches the specified size.
     *
     * @param expectedSize the expected file size in bytes
     * @return a matcher that returns true if the file size matches the expected size
     */
    public static Matcher<Path> size(long expectedSize) {
        return new PathSizeEqualsMatcher(expectedSize);
    }

    /**
     * Returns a matcher that checks if the file size is greater than the specified size.
     *
     * @param expectedSize the expected file size in bytes
     * @return a matcher that returns true if the file size is greater than the expected size
     */
    public static Matcher<Path> sizeGreaterThan(long expectedSize) {
        return new PathSizeGreaterThanMatcher(expectedSize);
    }

    /**
     * Returns a matcher that checks if the file size is less than the specified size.
     *
     * @param expectedSize the expected file size in bytes
     * @return a matcher that returns true if the file size is less than the expected size
     */
    public static Matcher<Path> sizeLessThan(long expectedSize) {
        return new PathSizeLessThanMatcher(expectedSize);
    }

    /**
     * Returns a matcher that checks if the path is executable.
     *
     * @return a matcher that returns true if the path is executable
     */
    public static Matcher<Path> isExecutable() {
        return new PathIsExecutableMatcher();
    }

    /**
     * Returns a matcher that checks if the file name matches the given regular expression.
     *
     * @param regex the regular expression to match the file name against
     * @return a matcher that returns true if the file name matches the given regular expression
     */
    public static Matcher<Path> matchesPattern(String regex) {
        return new PathMatchesPatternMatcher(regex);
    }

    // Matchers implementations
    private static class PathExistsMatcher implements Matcher<Path> {
        @Override
        public  boolean matches(Path path) {
            return Files.exists(path);
        }
    }

    private static class PathIsDirectoryMatcher implements Matcher<Path> {
        @Override
        public  boolean matches(Path path) {
            return Files.isDirectory(path);
        }
    }

    private static class PathIsFileMatcher implements Matcher<Path> {
        @Override
        public  boolean matches(Path path) {
            return Files.isRegularFile(path);
        }
    }

    private record PathHasExtensionMatcher(String extension) implements Matcher<Path> {
        @Override
        public boolean matches(Path path) {
            String fileName = path.getFileName().toString();
            return fileName.endsWith("." + extension);
        }
    }

    private static class PathIsReadableMatcher implements Matcher<Path> {
        @Override
        public  boolean matches(Path path) {
            return Files.isReadable(path);
        }
    }

    private static class PathIsWritableMatcher implements Matcher<Path> {
        @Override
        public  boolean matches(Path path) {
            return Files.isWritable(path);
        }
    }

    private record PathSizeEqualsMatcher(long expectedSize) implements Matcher<Path> {
        @Override
        public boolean matches(Path path) {
            try {
                return Files.size(path) == expectedSize;
            } catch (Exception e) {
                return false;
            }
        }
    }

    private record PathSizeGreaterThanMatcher(long expectedSize) implements Matcher<Path> {
        @Override
        public boolean matches(Path path) {
            try {
                return Files.size(path) > expectedSize;
            } catch (Exception e) {
                return false;
            }
        }
    }

    private record PathSizeLessThanMatcher(long expectedSize) implements Matcher<Path> {
        @Override
        public boolean matches(Path path) {
            try {
                return Files.size(path) < expectedSize;
            } catch (Exception e) {
                return false;
            }
        }
    }

    private static class PathIsExecutableMatcher implements Matcher<Path> {
        @Override
        public  boolean matches(Path path) {
            return Files.isExecutable(path);
        }
    }

    private static class PathMatchesPatternMatcher implements Matcher<Path> {
        private final Pattern pattern;

        public PathMatchesPatternMatcher(String regex) {
            this.pattern = Pattern.compile(regex);
        }

        @Override
        public  boolean matches(Path path) {
            return pattern.matcher(path.getFileName().toString()).matches();
        }
    }
}

