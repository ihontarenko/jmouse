package org.jmouse.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The records one call would touch, resolved before it touches them.
 *
 * <p>This one value feeds three guards, which is why it is resolved once and carried rather than
 * recomputed: the per-call ceiling reads {@link #totalCount()}, the confirmation threshold reads the
 * same number, and the preview shows {@link #records()}. Resolving it twice is not merely wasteful —
 * the second answer can differ from the first, and a preview that promised one set while the work
 * touched another is the failure two-step confirmation exists to prevent.
 *
 * <p><strong>{@code totalCount} and {@code records} are separate on purpose.</strong> A call that
 * would affect four thousand records must be refusable <em>by count</em> without four thousand rows
 * being loaded to say so. A resolver is free to stop fetching once it knows the call is over the
 * ceiling; the count still tells the truth, and the guard that refuses reads only the count.
 *
 * @param records    the records themselves, labelled — possibly a capped prefix of the whole set
 * @param totalCount how many there really are
 */
public record AffectedRecords(List<Record> records, long totalCount) {

    public AffectedRecords {
        records = records == null ? List.of() : List.copyOf(records);
    }

    /**
     * One record, identified, named, and — where the resolver could see it — as it stands right now.
     *
     * <p>{@code previousState} is what a trail keeps when the record stops existing. For a product
     * with no soft delete it is the only trace of what disappeared: without it the trail says
     * <em>"twelve records were deleted"</em> and what they were is gone for good. It is captured here
     * rather than at the point of deletion because here is where the rows are already loaded — the
     * resolver walks them to produce the label either way, so the state costs nothing extra, and the
     * moment after this one is too late.
     *
     * @param id            what the handler will act on
     * @param label         what a person reads in a preview
     * @param kind          what sort of thing it is, in the user's words; may be null
     * @param previousState field name to value, empty when the resolver had nothing to snapshot
     */
    public record Record(String id, String label, String kind, Map<String, String> previousState) {

        public Record {
            previousState = previousState == null ? Map.of() : Map.copyOf(previousState);
        }

        /** A record whose contents are not worth keeping — anything that is not being destroyed. */
        public static Record of(String id, String label, String kind) {
            return new Record(id, label, kind, Map.of());
        }

        public static Record of(String id, String label, String kind, Map<String, String> previousState) {
            return new Record(id, label, kind, previousState);
        }

        /** How one record reads in a preview: what it is, before how many there are. */
        public Map<String, Object> describe() {
            Map<String, Object> described = new LinkedHashMap<>();
            described.put("id", id);
            described.put("label", label);
            if (kind != null) {
                described.put("kind", kind);
            }
            return described;
        }
    }

    /**
     * An action whose reach is not knowable in advance — a create, which affects nothing that exists
     * yet. Counted as zero, so neither the ceiling nor the confirmation threshold has anything to
     * fire on.
     */
    public static AffectedRecords none() {
        return new AffectedRecords(List.of(), 0);
    }

    public static AffectedRecords of(List<Record> records) {
        return new AffectedRecords(records, records == null ? 0 : records.size());
    }

    /** As above, but knowing the set was capped, so the real count comes from elsewhere. */
    public static AffectedRecords capped(List<Record> records, long totalCount) {
        return new AffectedRecords(records, totalCount);
    }

    public List<String> identifiers() {
        return records.stream().map(Record::id).toList();
    }

    public boolean isEmpty() {
        return totalCount == 0;
    }

    /** The preview body, in the order a reader wants it. */
    public List<Map<String, Object>> describe() {
        return records.stream().map(Record::describe).toList();
    }
}
