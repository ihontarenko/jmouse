package org.jmouse.dom.renderer;

import org.jmouse.dom.TagName;

import java.util.Set;

import static org.jmouse.dom.TagName.*;

/**
 * Immutable rendering configuration used by DOM renderers. ⚙️
 *
 * <p>
 * {@code RendererContext} defines formatting and escaping behavior
 * during serialization of a DOM tree.
 * </p>
 *
 * <h3>Configuration options</h3>
 * <ul>
 *     <li>{@link #indentUnit()} — indentation string per depth level</li>
 *     <li>{@link #prettyPrint()} — enables indentation and newlines</li>
 *     <li>{@link #escapeMode()} — controls text/attribute escaping</li>
 *     <li>{@link #voidTags()} — HTML void elements (no closing tag)</li>
 * </ul>
 *
 * <h3>Escape modes</h3>
 * <ul>
 *     <li>{@link EscapeMode#NONE} — no escaping</li>
 *     <li>{@link EscapeMode#HTML} — HTML-style escaping</li>
 *     <li>{@link EscapeMode#XML} — XML-style escaping</li>
 * </ul>
 *
 * <h3>Usage example</h3>
 *
 * <pre>{@code
 * RendererContext context = RendererContext.builder()
 *     .escapeMode(RendererContext.EscapeMode.HTML)
 *     .prettyPrint(true)
 *     .indentUnit("  ")
 *     .build();
 * }</pre>
 *
 * <p>
 * Instances are immutable and thread-safe.
 * </p>
 */
public final class RendererContext {

    /**
     * Escaping mode used during rendering.
     */
    public enum EscapeMode {
        /**
         * No escaping is performed.
         */
        NONE,

        /**
         * HTML escaping rules are applied.
         */
        HTML,

        /**
         * XML escaping rules are applied.
         */
        XML
    }

    private final String     indentUnit;
    private final EscapeMode escapeMode;
    private final boolean    prettyPrint;

    /**
     * HTML void elements that should not have closing tags.
     * Used only when {@link EscapeMode#HTML} is active.
     */
    private final Set<TagName> voidTags;

    private RendererContext(Builder builder) {
        this.indentUnit  = builder.indentUnit;
        this.prettyPrint = builder.prettyPrint;
        this.escapeMode  = builder.escapeMode;
        this.voidTags    = Set.copyOf(builder.voidTags);
    }

    /**
     * Returns indentation unit used per depth level.
     *
     * @return indentation string (possibly empty)
     */
    public String indentUnit() {
        return indentUnit;
    }

    /**
     * Indicates whether pretty-print formatting is enabled.
     *
     * @return {@code true} if indentation and newlines are applied
     */
    public boolean prettyPrint() {
        return prettyPrint;
    }

    /**
     * Returns active escaping mode.
     *
     * @return escape mode
     */
    public EscapeMode escapeMode() {
        return escapeMode;
    }

    /**
     * Returns configured set of HTML void tags.
     *
     * <p>
     * Used only when {@link #escapeMode()} is {@link EscapeMode#HTML}.
     * </p>
     *
     * @return immutable set of void tag names
     */
    public Set<TagName> voidTags() {
        return voidTags;
    }

    /**
     * Creates a new builder for {@link RendererContext}.
     *
     * @return builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns default HTML context with pretty printing enabled.
     *
     * @return default HTML pretty-print context
     */
    public static RendererContext defaultsHtmlPretty() {
        return builder()
                .escapeMode(EscapeMode.HTML)
                .prettyPrint(true)
                .build();
    }

    /**
     * Returns default XML context with pretty printing enabled.
     *
     * @return default XML pretty-print context
     */
    public static RendererContext defaultsXmlPretty() {
        return builder()
                .escapeMode(EscapeMode.XML)
                .prettyPrint(true)
                .build();
    }

    /**
     * Builder for {@link RendererContext}. 🏗️
     *
     * <p>
     * Provides customizable formatting and escaping configuration.
     * </p>
     */
    public static final class Builder {

        private String     indentUnit  = "\t";
        private boolean    prettyPrint = true;
        private EscapeMode escapeMode  = EscapeMode.HTML;

        private Set<TagName> voidTags = Set.of(
                AREA, BASE, BR, COL, EMBED, HR, IMG, INPUT, LINK, META, PARAM, SOURCE, TRACK, WBR
        );

        /**
         * Sets indentation unit string.
         *
         * @param indentUnit indentation string per level (null → empty string)
         * @return this builder
         */
        public Builder indentUnit(String indentUnit) {
            this.indentUnit = indentUnit == null ? "" : indentUnit;
            return this;
        }

        /**
         * Enables or disables pretty printing.
         *
         * @param prettyPrint whether to apply indentation/newlines
         * @return this builder
         */
        public Builder prettyPrint(boolean prettyPrint) {
            this.prettyPrint = prettyPrint;
            return this;
        }

        /**
         * Sets escape mode.
         *
         * @param escapeMode escape mode (null → {@link EscapeMode#NONE})
         * @return this builder
         */
        public Builder escapeMode(EscapeMode escapeMode) {
            this.escapeMode = escapeMode == null ? EscapeMode.NONE : escapeMode;
            return this;
        }

        /**
         * Overrides the set of HTML void tags.
         *
         * @param voidTags set of tags treated as void (null → empty set)
         * @return this builder
         */
        public Builder voidTags(Set<TagName> voidTags) {
            this.voidTags = voidTags == null ? Set.of() : voidTags;
            return this;
        }

        /**
         * Builds immutable {@link RendererContext}.
         *
         * @return new renderer context
         */
        public RendererContext build() {
            return new RendererContext(this);
        }
    }
}