package org.jmouse.parallax.catalogue;

import org.jmouse.parallax.object.ObjectKind;

import java.util.Optional;

/**
 * The catalogue section an object of a kind is filed in when nobody chose one — a formula typed into a set, an
 * object imported from a file. Curated objects choose their own section; this is only the default.
 *
 * <p>Parallax seeds no sections, so a host whose sections differ replaces this policy; a kind it has no home for is
 * filed under {@link #GENERAL}.</p>
 */
public interface HomeCategoryPolicy {

    /** The section every host is expected to have: where an object goes that has no better home. */
    String GENERAL = "GENERAL";

    /** The code of the section objects of this kind are filed in by default; empty when the host has none for it. */
    Optional<String> homeOf(ObjectKind kind);

    /** The section to file an object of this kind in: its home, or {@link #GENERAL}. */
    default String categoryOf(ObjectKind kind) {
        return homeOf(kind).orElse(GENERAL);
    }
}
