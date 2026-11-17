package org.jmouse.cad.live;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.json.patch.JsonPatch;
import org.jmouse.json.patch.JsonPatchOperation;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The layouts a pcb's session holds, read: each slot's tree and the layout read from it, so an ask reads a layout once, not at every
 * step of a hand. A pcb a patch changed only inside some of its tracks — the points of the track carried, as every step of a drag
 * sends — reads those tracks again; the rest of it is the very pcb read before. A pcb of thousands of tracks was read whole again
 * at every step, and that was most of what a step cost the server.
 *
 * <p>Read and written on the socket's own thread alone, as the trees are.</p>
 */
final class LayoutReading {

    /** A track's place in a pcb's tree, and what of it a patch reaches past the track itself. */
    private static final Pattern TRACK_PATH = Pattern.compile("^/tracks/(\\d+)(/.*)?$");

    /** A slot's tree and the layout read from it. */
    private record Kept(JsonNode tree, Object read) {
    }

    private final ObjectMapper      mapper;
    private final Map<String, Kept> kept = new HashMap<>();

    LayoutReading(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * The layout a slot now holds, read as {@code type}: the one read before where its tree is the very tree; a pcb a patch changed
     * only inside its tracks, those tracks read again over the pcb read before; anything else read whole.
     */
    Object read(String slot, JsonNode tree, Optional<JsonPatch> patch, Class<?> type) {
        Kept before = kept.get(slot);
        Object read = before != null && before.tree() == tree && type.isInstance(before.read()) ? before.read() : null;
        if (read == null && before != null && type == PcbDrawing.class && before.read() instanceof PcbDrawing drawn && patch.isPresent()) {
            read = tracksPatched(drawn, tree, patch.get()).orElse(null);
        }
        if (read == null) {
            read = mapper.treeToValue(tree, type);
        }
        kept.put(slot, new Kept(tree, read));
        return read;
    }

    /** A slot's layout let go: the next one sent to it is read whole. */
    void forget(String slot) {
        kept.remove(slot);
    }

    void clear() {
        kept.clear();
    }

    /**
     * The pcb read before with the tracks a patch changed inside read again from the tree it left; empty where it did more — a track
     * added or taken away, anything but tracks changed —, and the pcb is read whole.
     */
    private Optional<PcbDrawing> tracksPatched(PcbDrawing before, JsonNode tree, JsonPatch patch) {
        JsonNode tracks = tree.path("tracks");
        if (!tracks.isArray() || tracks.size() != before.tracks().size()) {
            return Optional.empty();
        }
        Set<Integer> touched = new TreeSet<>();
        for (JsonPatchOperation operation : patch.operations()) {
            Optional<Integer> track = trackChangedBy(operation, before.tracks().size());
            if (track.isEmpty()) {
                return Optional.empty();
            }
            touched.add(track.get());
        }
        List<PcbDrawing.Track> now = new ArrayList<>(before.tracks());
        for (int index : touched) {
            now.set(index, mapper.treeToValue(tracks.get(index), PcbDrawing.Track.class));
        }
        return Optional.of(before.withTracks(List.copyOf(now)));
    }

    /** The track an operation changes inside, or replaces whole; empty for an operation that does anything else. */
    private static Optional<Integer> trackChangedBy(JsonPatchOperation operation, int tracks) {
        String path;
        boolean inside;
        switch (operation) {
            case JsonPatchOperation.Replace replace -> {
                path = replace.path().toString();
                inside = false;
            }
            case JsonPatchOperation.Add add -> {
                path = add.path().toString();
                inside = true;
            }
            case JsonPatchOperation.Remove remove -> {
                path = remove.path().toString();
                inside = true;
            }
            default -> {
                return Optional.empty();
            }
        }
        Matcher place = TRACK_PATH.matcher(path);
        if (!place.matches()) {
            return Optional.empty();
        }
        // A track added or taken away moves every one after it: only a change inside one, or one replaced whole, keeps their places.
        if (inside && place.group(2) == null) {
            return Optional.empty();
        }
        int index = Integer.parseInt(place.group(1));
        return index < tracks ? Optional.of(index) : Optional.empty();
    }
}
