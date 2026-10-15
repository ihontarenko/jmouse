package org.jmouse.parallax.store.catalogue;

import org.jmouse.parallax.authority.ParallaxAuthority;
import org.jmouse.parallax.authority.ParallaxCapability;
import org.jmouse.parallax.jpa.catalogue.Instrument;
import org.jmouse.parallax.jpa.catalogue.InstrumentStore;
import org.jmouse.parallax.jpa.catalogue.MathematicalObject;
import org.jmouse.parallax.jpa.catalogue.ObjectStore;
import org.jmouse.parallax.store.catalogue.CatalogueViews.InstrumentView;
import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.object.ObjectKind;
import org.jmouse.parallax.problem.ParallaxProblem;
import org.jmouse.parallax.store.support.ParallaxJson;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** The visualizers and labs of the portal: listing them, and their editors changing what each opens with. */
public class Instruments {

    private static final TypeReference<List<RepresentationKind>> KINDS = new TypeReference<>() {
    };

    private final InstrumentStore instruments;
    private final ObjectStore objects;
    private final ParallaxAuthority authority;
    private final ParallaxJson documents;

    public Instruments(InstrumentStore instruments, ObjectStore objects, ParallaxAuthority authority,
                       ParallaxJson documents) {
        this.instruments = instruments;
        this.objects = objects;
        this.authority = authority;
        this.documents = documents;
    }

    public record InstrumentDraft(Instrument.Section section, String title, String summary, String description,
                                  Set<ObjectKind> objectKinds, List<RepresentationKind> defaultRepresentations,
                                  String presetObjectSlug, boolean enabled, int position) {
    }

    @Transactional(readOnly = true)
    public List<InstrumentView> list(Instrument.Section section) {
        boolean editor = authority.permitsGlobally(ParallaxCapability.CATALOGUE_WRITE);
        return instruments.allInOrder().stream()
                .filter(instrument -> section == null || instrument.getSection() == section)
                .filter(instrument -> instrument.isEnabled() || editor)
                .map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public InstrumentView get(String code) {
        Instrument instrument = instruments.find(code).filter(found -> found.isEnabled()
                        || authority.permitsGlobally(ParallaxCapability.CATALOGUE_WRITE))
                .orElseThrow(() -> ParallaxProblem.notFound("The instrument"));
        return view(instrument);
    }

    @Transactional
    public InstrumentView save(String code, InstrumentDraft draft) {
        authority.requireGlobally(ParallaxCapability.CATALOGUE_WRITE);
        if (code == null || !code.matches("[A-Z][A-Z0-9_]{1,39}")) {
            throw ParallaxProblem.invalid("An instrument code is uppercase words joined by underscores.");
        }
        if (draft.title() == null || draft.title().isBlank() || draft.summary() == null || draft.summary().isBlank()) {
            throw ParallaxProblem.invalid("An instrument needs a title and a summary.");
        }
        return view(instruments.save(describe(instruments.find(code)
                .orElseGet(() -> new Instrument(code, draft.section())), draft)));
    }

    /**
     * The bootstrap's path: an instrument the shipped catalogue holds, written once and never rewritten — an editor
     * may have changed what it opens with since.
     *
     * @return whether the instrument was written now
     */
    @Transactional
    public boolean seed(String code, InstrumentDraft draft) {
        if (instruments.exists(code)) {
            return false;
        }
        instruments.save(describe(new Instrument(code, draft.section()), draft));
        return true;
    }

    private Instrument describe(Instrument instrument, InstrumentDraft draft) {
        String presetObjectId = draft.presetObjectSlug() == null ? null : objects.findBySlug(draft.presetObjectSlug())
                .orElseThrow(() -> ParallaxProblem.invalid("No object is called '" + draft.presetObjectSlug() + "'.")).getId();
        Set<String> kinds = new LinkedHashSet<>();
        (draft.objectKinds() == null ? Set.<ObjectKind>of() : draft.objectKinds()).forEach(kind -> kinds.add(kind.name()));
        instrument.describe(draft.title().strip(), draft.summary().strip(), draft.description(),
                documents.write(draft.defaultRepresentations() == null ? List.of() : draft.defaultRepresentations()),
                presetObjectId, draft.enabled(), draft.position(), kinds);
        return instrument;
    }

    private InstrumentView view(Instrument instrument) {
        String presetSlug = instrument.getPresetObjectId() == null ? null
                : objects.find(instrument.getPresetObjectId()).map(MathematicalObject::getSlug).orElse(null);
        return new InstrumentView(instrument.getCode(), instrument.getSection(), instrument.getTitle(), instrument.getSummary(),
                instrument.getDescription(), Set.copyOf(instrument.getObjectKinds()),
                documents.read(instrument.getDefaultRepresentations(), KINDS), presetSlug, instrument.isEnabled(),
                instrument.getPosition());
    }
}
