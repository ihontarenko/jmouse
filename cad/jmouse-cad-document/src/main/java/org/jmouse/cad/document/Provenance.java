package org.jmouse.cad.document;

import org.jmouse.el.grammar.Nodes;

import java.util.Optional;

/**
 * 🏷️ Where a document came from, whose it is and under what terms — the same three lines of {@code meta} for anything
 * the engine holds, whoever made it: a symbol converted from KiCad's libraries, a footprint a supplier handed over, a 3D
 * model, or one of our own.
 *
 * <p>⚠️ Data taken from elsewhere is taken openly: whatever converts or copies another's work writes this, and nothing
 * strips it, so every adaptation keeps saying whose it was and keeps its licence. A document that names no holder is
 * ours.</p>
 *
 * @param source  where it was taken from, as the source names it — {@code KiCad footprint Package_SO:SOIC-8_…},
 *                a supplier's part page —; null when it was made here
 * @param authors who holds it — {@code KiCad Libraries Team} —; null when it is ours
 * @param license its licence as an SPDX expression — {@code CC-BY-SA-4.0 WITH KiCad-libraries-exception} —; null when
 *                none is stated
 */
public record Provenance(String source, String authors, String license) {

    /** Made here, claiming nobody else's work. */
    public static final Provenance OURS = new Provenance(null, null, null);

    /** The provenance a document's {@code meta} states; {@link #OURS} when it states none. */
    public static Provenance of(DocumentNode document) {
        return document.block(MetaNode.class).map(Provenance::of).orElse(OURS);
    }

    /** The provenance a {@code meta} block states. */
    public static Provenance of(MetaNode meta) {
        return new Provenance(meta.text(MetaNode.SOURCE).orElse(null), meta.text(MetaNode.AUTHORS).orElse(null),
                meta.text(MetaNode.LICENSE).orElse(null));
    }

    /** Whether it is someone else's work: a holder or a licence is named. */
    public boolean borrowed() {
        return authors != null || license != null;
    }

    /** The same holder and terms for another of their works. */
    public Provenance from(String otherSource) {
        return new Provenance(otherSource, authors, license);
    }

    /** Its lines added to {@code meta}, each only when stated: {@code source}, {@code authors}, {@code license}. */
    public void writeInto(MetaNode meta) {
        Optional.ofNullable(source).ifPresent(text -> meta.addExpression(MetaLineNode.of(MetaNode.SOURCE, Nodes.text(text))));
        Optional.ofNullable(authors).ifPresent(text -> meta.addExpression(MetaLineNode.of(MetaNode.AUTHORS, Nodes.text(text))));
        Optional.ofNullable(license).ifPresent(text -> meta.addExpression(MetaLineNode.of(MetaNode.LICENSE, Nodes.text(text))));
    }
}
