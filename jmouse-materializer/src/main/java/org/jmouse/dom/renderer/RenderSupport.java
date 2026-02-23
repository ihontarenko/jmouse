package org.jmouse.dom.renderer;

import org.jmouse.dom.TagName;

import static org.jmouse.util.Strings.emptyIfNull;

/**
 * Internal rendering utilities for DOM serialization. 🛠️
 *
 * <p>
 * Provides low-level helpers used by renderers:
 * </p>
 * <ul>
 *     <li>indentation and newline handling (pretty-print mode)</li>
 *     <li>text and attribute escaping (HTML/XML/NONE)</li>
 *     <li>void tag detection</li>
 * </ul>
 *
 * <p>
 * This class is package-private and intended for renderer infrastructure only.
 * </p>
 */
final class RenderSupport {

    private RenderSupport() {}

    /**
     * Returns indentation string for a given depth.
     *
     * <p>
     * If {@link RendererContext#prettyPrint()} is disabled,
     * an empty string is returned.
     * </p>
     *
     * @param depth node depth in the tree
     * @param context renderer context
     * @return indentation string (possibly empty)
     */
    static String indent(int depth, RendererContext context) {
        if (!context.prettyPrint()) {
            return "";
        }

        String unit = context.indentUnit();

        if (unit.isEmpty() || depth <= 0) {
            return "";
        }

        return unit.repeat(depth);
    }

    /**
     * Returns newline string depending on pretty-print mode.
     *
     * @param context renderer context
     * @return {@code "\n"} if pretty printing is enabled, otherwise empty string
     */
    static String newline(RendererContext context) {
        return context.prettyPrint() ? "\n" : "";
    }

    /**
     * Escapes text content according to the current escape mode.
     *
     * <ul>
     *     <li>{@code NONE} — returns value as-is (null-safe)</li>
     *     <li>{@code HTML/XML} — escapes {@code &, <, >}</li>
     * </ul>
     *
     * @param value raw text value (nullable)
     * @param context renderer context
     * @return escaped text (never {@code null})
     */
    static String escapeText(String value, RendererContext context) {
        return switch (context.escapeMode()) {
            case NONE -> emptyIfNull(value);
            case HTML, XML -> emptyIfNull(value)
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
        };
    }

    /**
     * Escapes attribute value according to escape mode.
     *
     * <p>
     * Performs text escaping and additionally escapes:
     * {@code "} → {@code &quot;} and {@code '} → {@code &apos;}.
     * </p>
     *
     * @param raw raw attribute value (nullable)
     * @param context renderer context
     * @return escaped attribute value (never {@code null})
     */
    static String escapeAttribute(String raw, RendererContext context) {
        return escapeText(raw, context)
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Determines whether the given tag should be treated as a void element.
     *
     * <p>
     * A tag is considered void only when:
     * </p>
     * <ul>
     *     <li>escape mode is {@link RendererContext.EscapeMode#HTML}</li>
     *     <li>the tag is present in {@link RendererContext#voidTags()}</li>
     * </ul>
     *
     * @param tagName tag to check
     * @param context renderer context
     * @return {@code true} if tag is a void HTML element
     */
    static boolean isVoidTag(TagName tagName, RendererContext context) {
        return context.escapeMode() == RendererContext.EscapeMode.HTML
                && context.voidTags().contains(tagName);
    }
}