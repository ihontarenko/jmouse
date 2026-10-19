package org.jmouse.parallax.store.catalogue;

import org.jmouse.parallax.owner.ParallaxOwner;
import org.jmouse.parallax.jpa.catalogue.MathematicalObject;
import org.jmouse.parallax.jpa.catalogue.Instrument;
import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectKind;
import org.jmouse.parallax.object.ObjectNature;
import org.jmouse.parallax.object.ParameterDefinition;
import org.jmouse.parallax.store.formula.FormulaNotations.Notation;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** What the catalogue answers with. */
public final class CatalogueViews {

    private CatalogueViews() {
    }

    /** Where an object comes from, as the interface reads it: the shared catalogue, or an owner's own. */
    public enum Origin {
        CATALOGUE, OWNED;

        public static Origin of(MathematicalObject object) {
            return object.isCatalogue() ? CATALOGUE : OWNED;
        }
    }

    /** The owner an object is stored under; null for the catalogue. */
    public static ParallaxOwner ownerOf(MathematicalObject object) {
        return object.owner().orElse(null);
    }

    public record CategoryView(String code, String title, String description, long objectCount) {
    }

    public record ObjectSummary(String id, String slug, String title, String summary, ObjectKind kind, ObjectNature nature,
                                String category,
                                int ambientDimension, String formulaLatex, Notation notation, boolean featured,
                                MathematicalObject.Status status, Origin origin, Set<String> tags,
                                RepresentationKind primaryRepresentation, int definitionRevision, Instant updatedAt,
                                /** Why its text no longer reads, when the language moved on since it was saved; null when it reads. */
                                String problem) {
    }

    public record PropertyView(String key, String label, String value) {
    }

    public record RepresentationView(RepresentationKind kind, boolean primary, Map<String, Object> settings) {
    }

    /** A preset's {@code id} is its name as a slug: stable for as long as the preset keeps its name. */
    public record PresetView(String id, String name, String description, Map<String, Double> values,
                             Map<String, Object> camera) {
    }

    public record RelatedView(String slug, String title, ObjectKind kind, String relation) {
    }

    /**
     * An object's page: its jMX text and everything read from it. The definition, sliders, properties,
     * representations and presets are derived from {@code source}; editing either side edits one document.
     *
     * @param definitionRevision    the head revision of the text, kept under its old name for the interface
     * @param source                the head revision's jMX text — the truth
     * @param sourceRevision        the revision {@code source} is
     * @param shippedUpdatePending  whether the shipped catalogue has a newer text, kept back because a curator edited
     *                              this object
     * @param explanation           the structure and what is derived from it, each with its line; null for a formula
     *                              that is not saved
     */
    public record ObjectDetail(ObjectSummary summary, String description, ObjectDefinition definition,
                               int definitionRevision, List<ParameterDefinition> parameters,
                               List<PropertyView> properties, List<RepresentationView> representations,
                               List<RepresentationKind> applicableRepresentations, List<PresetView> presets,
                               List<RelatedView> related, ParallaxOwner owner, long version, String source,
                               int sourceRevision, boolean shippedUpdatePending,
                               org.jmouse.parallax.script.engine.ObjectExplanation explanation) {
    }

    /** One saved revision of an object's text; {@code writtenBy} is null for the shipped catalogue. */
    public record RevisionView(int revision, String checksum, Instant writtenAt, String writtenBy) {
    }

    /** One revision with its text. */
    public record RevisionSource(int revision, String source) {
    }

    public record InstrumentView(String code, Instrument.Section section, String title, String summary,
                                 String description, Set<String> objectKinds,
                                 List<RepresentationKind> defaultRepresentations, String presetObjectSlug,
                                 boolean enabled, int position) {
    }
}
