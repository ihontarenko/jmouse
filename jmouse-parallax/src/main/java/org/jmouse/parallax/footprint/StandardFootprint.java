package org.jmouse.parallax.footprint;

import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.engine.Notation;
import org.jmouse.parallax.script.writer.DocumentLines;

import java.util.List;

/**
 * A footprint of our library generated from a standard's table: its name, the folder it is filed in, how it was worked
 * out, and its pads and outlines.
 *
 * @param folder   where under {@code catalogue/electronics/footprints} it is filed — the category it shows in
 * @param mounting {@code surface} or {@code through-hole}
 * @param source   the standard its dimensions were read from
 * @param notes    how its copper was worked out, written above its lines for a reviewer
 */
public record StandardFootprint(String slug, String title, String summary, String folder, String mounting, String source,
                                String license, List<String> notes, Block.Footprint drawing) {

    public StandardFootprint {
        notes = List.copyOf(notes);
    }

    /** The footprint as a jMX document, as the catalogue ships it. */
    public String document() {
        StringBuilder text = new StringBuilder();
        text.append("footprint ").append(Notation.text(title)).append(" as ").append(slug).append(" {\n");
        text.append("    meta {\n");
        text.append("        summary ").append(Notation.text(summary)).append('\n');
        text.append("        category circuits\n");
        text.append("        tags electronics, footprint, ").append(mounting).append('\n');
        text.append("        source ").append(Notation.text(source)).append('\n');
        text.append("        license ").append(Notation.text(license)).append('\n');
        text.append("    }\n");
        for (String note : notes) {
            text.append("    # ").append(note).append('\n');
        }
        for (String line : DocumentLines.footprintLines(drawing)) {
            text.append("    ").append(line).append('\n');
        }
        return text.append("}\n").toString();
    }
}
