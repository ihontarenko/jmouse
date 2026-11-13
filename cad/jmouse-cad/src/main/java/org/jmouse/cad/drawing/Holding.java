package org.jmouse.cad.drawing;

import java.util.List;
import java.util.Optional;

/**
 * Whether a drawing written anew keeps what was held where it stands: every item held before — locked itself, or in a locked
 * group — is still there, as it was. An item freed in the same writing (its name now standing free) may change: unlocking is the
 * reader's own act. Asked by the engine whenever a drawing is saved, whatever wrote it — a tool, the text editor, an API call.
 */
public final class Holding {

    private Holding() {
    }

    /** Why the drawing written anew may not be kept, naming the held item it moved or removed; empty when it keeps them all. */
    public static Optional<String> refusal(ArrangedDrawing before, ArrangedDrawing after) {
        List<StandingItem> written = after.standingItems();
        DrawnGroups groupsAfter = after.drawnGroups();
        for (StandingItem item : before.standingItems()) {
            Optional<String> holder = before.drawnGroups().holder(item.arrangement());
            if (holder.isEmpty()) {
                continue;
            }
            boolean kept = written.stream().anyMatch(candidate -> candidate.standing().equals(item.standing()));
            boolean freed = written.stream().anyMatch(candidate -> candidate.name().equals(item.name()) && !groupsAfter.holds(candidate.arrangement()));
            if (!kept && !freed) {
                return Optional.of(item.name() + " is held where it stands — " + holder.get() + ": unlock it before moving or removing it");
            }
        }
        return Optional.empty();
    }
}
