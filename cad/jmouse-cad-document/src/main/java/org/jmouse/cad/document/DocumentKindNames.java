package org.jmouse.cad.document;

import org.jmouse.settings.Label;
import org.jmouse.cad.settings.SettingsSection;

import java.util.Map;
import java.util.Optional;

/**
 * How each kind of document is spoken of, by its word in the language: one of it in a sentence, several as a
 * heading names them, and — for a kind named by a technical code — the kind its codes end in ({@code F} of
 * {@code CHIP:R0805:F}). A kind no entry names — a plugin's — is spoken of by its word.
 *
 * @param kinds every kind's names, by its word: {@code footprint}, {@code pcb}
 */
public record DocumentKindNames(@Label("Kinds") Map<String, Names> kinds) {

    /** Its section of the settings tree (ADR 0004): {@code org/jmouse/cad/settings/library/kinds}. */
    public static final SettingsSection<DocumentKindNames> SECTION = new SettingsSection<>("library/kinds", "Document kinds", DocumentKindNames.class);

    public DocumentKindNames {
        kinds = Map.copyOf(kinds);
    }

    /**
     * One kind's names.
     *
     * @param one    one of it, in a sentence: {@code footprint}, {@code 3D model}
     * @param plural several, as a heading names them: {@code Footprints}
     * @param code   the kind its technical codes end in: {@code F}; empty for a kind named by a slug
     */
    public record Names(@Label("One") String one, @Label("Plural") String plural, @Label("Code") Optional<String> code) {
    }
}
