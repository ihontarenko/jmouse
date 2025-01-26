package svit.env;

/**
 * Resolves placeholders in strings (e.g., {@code ${app.name}}`).
 */
public interface PlaceholderResolver {

    /**
     * Resolves placeholders in the given text.
     */
    String resolvePlaceholders(String text);

    /**
     * Resolves placeholders, throwing an exception if any remain unresolved.
     */
    String resolveRequiredPlaceholders(String text);

}