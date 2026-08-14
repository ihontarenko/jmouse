package org.jmouse.ai.sandbox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A dozen records in three maps. The domain is not the point.
 *
 * <p>What it is shaped for is the awkward cases: three workshops of which two share a name, one caller
 * who can see one of them and one who can see all three, enough parts on one shelf to cross a
 * confirmation threshold and enough in one workshop to cross a ceiling, and a set of notes that belong
 * to a subject rather than to any workshop.
 */
public final class WorkshopInventory {

    public static final String BENCH         = "bench";
    public static final String GARAGE        = "garage";
    public static final String GARAGE_ANNEXE = "garage-annexe";

    private final Map<String, Workshop> workshops = new LinkedHashMap<>();
    private final Map<String, Part>     parts     = new LinkedHashMap<>();
    private final Map<String, Note>     notes     = new LinkedHashMap<>();
    private final AtomicInteger         sequence  = new AtomicInteger();

    public WorkshopInventory() {
        workshops.put(BENCH,         new Workshop(BENCH,         "Bench"));
        workshops.put(GARAGE,        new Workshop(GARAGE,        "Garage"));
        // Same name as the one above, on purpose: without it, ambiguity is a branch nobody reaches.
        workshops.put(GARAGE_ANNEXE, new Workshop(GARAGE_ANNEXE, "Garage"));

        // Eight on the bench, five of them on shelf A — enough to cross a threshold of three on one
        // shelf and a ceiling of six across the whole workshop.
        addPart(BENCH,  "A", "M3 bolt",        40);
        addPart(BENCH,  "A", "M4 bolt",        25);
        addPart(BENCH,  "A", "M5 bolt",        10);
        addPart(BENCH,  "A", "M3 nut",         60);
        addPart(BENCH,  "A", "M4 nut",         35);
        addPart(BENCH,  "B", "Bearing 608",     4);
        addPart(BENCH,  "B", "Bearing 6001",    2);
        addPart(BENCH,  "B", "Circlip 8mm",    12);
        addPart(GARAGE, "C", "Brake pad",       4);
        addPart(GARAGE, "C", "Oil filter",      2);
        addPart(GARAGE, "D", "Wiper blade",     3);
        addPart(GARAGE, "D", "Spark plug",      8);

        addNote(SandboxCallers.OWNER,        "Order more 6001 bearings");
        addNote(SandboxCallers.OWNER,        "The bench vice needs a new handle");
        addNote(SandboxCallers.SECOND_OWNER, "Garage annexe still has no lighting");
    }

    // ── Workshops ────────────────────────────────────────────────────────────────

    /**
     * Which workshops one identity can reach.
     *
     * <p>Keyed by the identity asked about rather than by "the caller", because the whole point of the
     * {@code workshops.list} action is that it asks about the <strong>caller</strong> where every other
     * action asks about the acting subject.
     */
    public List<Workshop> workshopsVisibleTo(String identityId) {
        return switch (identityId) {
            case SandboxCallers.ASSISTANT -> List.of(workshops.get(BENCH));
            case SandboxCallers.OWNER,
                 SandboxCallers.SECOND_OWNER -> List.copyOf(workshops.values());
            default -> List.of();
        };
    }

    public Optional<Workshop> workshop(String id) {
        return Optional.ofNullable(workshops.get(id));
    }

    // ── Parts ────────────────────────────────────────────────────────────────────

    public List<Part> partsIn(String workshopId) {
        return parts.values().stream().filter(part -> part.workshopId().equals(workshopId)).toList();
    }

    /** Parts in a workshop, optionally narrowed to one shelf. The filter three actions share. */
    public List<Part> partsMatching(String workshopId, String shelf) {
        return partsIn(workshopId).stream()
                .filter(part -> shelf == null || part.shelf().equalsIgnoreCase(shelf))
                .toList();
    }

    public Part addPart(String workshopId, String shelf, String name, int quantity) {
        Part part = new Part("part-" + sequence.incrementAndGet(), workshopId, name, shelf, quantity);
        parts.put(part.id(), part);
        return part;
    }

    public void adjustQuantity(String partId, int by) {
        parts.computeIfPresent(partId, (id, part) -> part.withQuantity(part.quantity() + by));
    }

    public void discard(String partId) {
        parts.remove(partId);
    }

    public int partCount() {
        return parts.size();
    }

    // ── Notes ────────────────────────────────────────────────────────────────────

    public List<Note> notesOf(String subjectId) {
        return notes.values().stream().filter(note -> note.subjectId().equals(subjectId)).toList();
    }

    public Note addNote(String subjectId, String text) {
        Note note = new Note("note-" + sequence.incrementAndGet(), subjectId, text);
        notes.put(note.id(), note);
        return note;
    }

    public void deleteNote(String noteId) {
        notes.remove(noteId);
    }
}
