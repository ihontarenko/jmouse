package org.jmouse.taxonomy;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * 📍 Where a category stands in its taxonomy, written as its slugs from the root: {@code semiconductors/diodes}.
 *
 * <p>A category is addressed by its path, not by an id, so a host and a person can both name one — a seed file says
 * {@code kicad/device}, a link says the same. Every slug is lowercase ASCII words joined by hyphens, made from the
 * category's title.</p>
 *
 * @param slugs the slugs from the root down, at least one
 */
public record CategoryPath(List<String> slugs) {

    /** What separates one slug from the next. */
    public static final String SEPARATOR = "/";

    private static final String SLUG = "[a-z0-9]+(-[a-z0-9]+)*";

    public CategoryPath {
        slugs = List.copyOf(slugs);
        if (slugs.isEmpty()) {
            throw new TaxonomyException.Invalid("A category's path names at least the category itself.");
        }
        for (String slug : slugs) {
            if (!slug.matches(SLUG)) {
                throw new TaxonomyException.Invalid("'" + slug + "' is not a slug: lowercase words and digits joined by "
                        + "hyphens.");
            }
        }
    }

    /**
     * 📖 A path as written: {@code semiconductors/diodes}.
     */
    public static CategoryPath parse(String written) {
        if (written == null || written.isBlank()) {
            throw new TaxonomyException.Invalid("A category's path is its slugs from the root, joined by '/'.");
        }
        return new CategoryPath(Arrays.stream(written.strip().split(SEPARATOR)).map(String::strip).toList());
    }

    /** One level below this one. */
    public CategoryPath child(String slug) {
        return new CategoryPath(Stream.concat(slugs.stream(), Stream.of(slug)).toList());
    }

    /** The category this one hangs under, or {@code null} for a root. */
    public CategoryPath parent() {
        return slugs.size() == 1 ? null : new CategoryPath(slugs.subList(0, slugs.size() - 1));
    }

    /** The category's own slug: the last of the path. */
    public String slug() {
        return slugs.getLast();
    }

    /** How deep it sits, a root being one, as the nested set counts. */
    public int depth() {
        return slugs.size();
    }

    /**
     * 🔤 A slug made from a title: accents dropped, lowercase, anything else between words a single hyphen.
     *
     * @throws TaxonomyException.Invalid when the title has no letter or digit to make one of
     */
    public static String slugOf(String title) {
        String decomposed = Normalizer.normalize(title == null ? "" : title, Normalizer.Form.NFKD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        String slug = decomposed.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (slug.isEmpty()) {
            throw new TaxonomyException.Invalid("'" + title + "' has no Latin letter or digit to address it by.");
        }
        return slug;
    }

    @Override
    public String toString() {
        return String.join(SEPARATOR, slugs);
    }
}
