package org.jmouse.parallax.script.writer;

import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Capability;
import org.jmouse.parallax.script.Meta;
import org.jmouse.parallax.script.Meta.MetaValue;

/** What describes and presents a document without changing what it computes: {@code meta} and {@code view}. */
final class PresentationWriter {

    private PresentationWriter() {
    }

    // ─── meta ───────────────────────────────────────────────────────────────────────────────────────

    /** The known fields in the order a document is usually written, then every free one; nothing when it says nothing. */
    static void meta(Meta meta, SourceLines lines) {
        if (meta.equals(Meta.EMPTY)) {
            return;
        }
        lines.block("meta", inner -> {
            if (meta.summary() != null) {
                inner.line("summary " + Values.longText(meta.summary(), inner.indentation()));
            }
            if (meta.description() != null) {
                inner.line("description " + Values.longText(meta.description(), inner.indentation()));
            }
            if (meta.category() != null) {
                inner.line("category " + meta.category());
            }
            if (!meta.tags().isEmpty()) {
                inner.line("tags " + Values.names(meta.tags()));
            }
            if (meta.source() != null) {
                inner.line("source " + Values.longText(meta.source(), inner.indentation()));
            }
            if (!meta.authors().isEmpty()) {
                inner.line("authors " + String.join(", ", meta.authors().stream().map(Values::text).toList()));
            }
            if (meta.license() != null) {
                inner.line("license " + Values.longText(meta.license(), inner.indentation()));
            }
            if (!meta.links().isEmpty()) {
                inner.block("links", links -> meta.links().forEach(link ->
                        links.line(Values.text(link.label()) + " " + Values.text(link.url()))));
            }
            meta.fields().forEach((name, value) -> {
                String label = meta.labels().get(name);
                String written = label == null && value instanceof MetaValue.Text text
                        ? Values.longText(text.text(), inner.indentation()) : value(value);
                inner.line(name + " " + written + (label == null ? "" : " " + Values.text(label)));
            });
        });
    }

    /** A free field's value, typed as it is read back: a text in quotes, a list, a span, a quantity, names. */
    private static String value(MetaValue value) {
        return switch (value) {
            case MetaValue.Text text -> Values.text(text.text());
            case MetaValue.Amount amount -> amount.quantity().written();
            case MetaValue.Span span -> span.minimum().written() + " .. " + span.maximum().written();
            case MetaValue.Names names -> Values.names(names.names());
            case MetaValue.Items items -> "[" + String.join(", ", items.items().stream().map(PresentationWriter::value).toList()) + "]";
        };
    }

    // ─── view ───────────────────────────────────────────────────────────────────────────────────────

    static void view(Block.View view, SourceLines lines) {
        lines.block("view", inner -> view.items().forEach(item -> viewItem(item, inner)));
    }

    private static void viewItem(Block.ViewItem item, SourceLines lines) {
        switch (item) {
            case Block.Representation representation -> lines.line(representation(representation));
            case Block.Preset preset -> {
                String opening = "preset " + Values.text(preset.title())
                        + (preset.description().isEmpty() ? "" : " " + Values.text(preset.description()));
                lines.block(opening, inner -> {
                    preset.values().forEach((name, value) -> inner.line(name + " = " + value.text()));
                    preset.camera().ifPresent(camera -> inner.line(camera(camera)));
                });
            }
            case Block.Camera camera -> lines.line(camera(camera));
            case Block.Explanation explanation -> lines.line("explain " + explanation.subject() + " "
                    + Values.text(explanation.text()));
            case Block.Without without -> lines.line("without " + Values.names(without.capabilities().stream()
                    .sorted().map(Capability::keyword).toList()));
        }
    }

    /** {@code contours primary levels = 20}: a representation, and its settings as written. */
    static String representation(Block.Representation representation) {
        StringBuilder line = new StringBuilder(representation.representation());
        if (representation.primary()) {
            line.append(" primary");
        }
        if (!representation.settings().isEmpty()) {
            line.append(' ').append(representation.settings().text());
        }
        return line.toString();
    }

    /** {@code camera azimuth 45 elevation 30}. */
    static String camera(Block.Camera camera) {
        StringBuilder line = new StringBuilder("camera");
        camera.settings().forEach((name, value) -> line.append(' ').append(name).append(' ').append(value.text()));
        return line.toString();
    }
}
