package org.jmouse.json.patch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 🩹 Where a JSON Patch changed a document, as a tree of the places it reached: a place changed whole — its value replaced, or what
 * it holds added to or taken from — or only somewhere below it. A reader of the patched document reads again what changed and keeps
 * everything else it read before ({@link StructuralReading}).
 *
 * <p>A value replaced changes its own place; one added, removed, moved or copied changes the place that holds it — the elements of an
 * array after it all move, so its holder is read whole.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ChangedPaths {

    private static final ChangedPaths NONE = new ChangedPaths();

    private final Map<String, ChangedPaths> below = new LinkedHashMap<>();
    private       boolean                   whole;

    private ChangedPaths() {
    }

    /** Nothing changed. */
    public static ChangedPaths none() {
        return NONE;
    }

    /** Where these operations of a patch change a document. */
    public static ChangedPaths of(Collection<JsonPatchChange> changes) {
        ChangedPaths root = new ChangedPaths();
        for (JsonPatchChange change : changes) {
            switch (change.operation()) {
                case REPLACE -> root.mark(pointer(change.path()));
                case ADD, REMOVE, COPY -> root.mark(holderOf(pointer(change.path())));
                case MOVE -> {
                    root.mark(holderOf(pointer(change.path())));
                    change.from().ifPresent(from -> root.mark(holderOf(pointer(from))));
                }
                case TEST -> {
                }
            }
        }
        return root;
    }

    /** Whether nothing here or below changed. */
    public boolean isEmpty() {
        return !whole && below.isEmpty();
    }

    /** Whether this place changed whole: what it holds is read again, not looked into. */
    public boolean isWhole() {
        return whole;
    }

    /**
     * Where a change reached below this place, by one step of a pointer — a member's name, an element's index —: below a place changed
     * whole, every place is changed whole too.
     */
    public ChangedPaths at(String step) {
        return whole ? this : below.getOrDefault(step, NONE);
    }

    private void mark(List<String> path) {
        ChangedPaths place = this;
        for (String step : path) {
            if (place.whole) {
                return;
            }
            place = place.below.computeIfAbsent(step, ignored -> new ChangedPaths());
        }
        place.whole = true;
        place.below.clear();
    }

    /** The place that holds a place: its pointer less the last step. */
    private static List<String> holderOf(List<String> path) {
        return path.isEmpty() ? path : path.subList(0, path.size() - 1);
    }

    /** A JSON Pointer's steps: {@code /a~1b/0} is {@code a/b}, then {@code 0}. */
    static List<String> pointer(String written) {
        List<String> steps = new ArrayList<>();
        if (written.isEmpty()) {
            return steps;
        }
        for (String step : written.substring(1).split("/", -1)) {
            steps.add(step.replace("~1", "/").replace("~0", "~"));
        }
        return steps;
    }
}
