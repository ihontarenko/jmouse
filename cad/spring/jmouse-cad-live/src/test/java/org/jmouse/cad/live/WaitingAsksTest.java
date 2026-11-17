package org.jmouse.cad.live;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaitingAsksTest {

    private static WaitingAsks.Waiting ask(long id, PcbAskKind kind) {
        return new WaitingAsks.Waiting(id, kind, "board", null, Map.of());
    }

    private static List<Long> takenInTurn(WaitingAsks waiting) {
        List<Long> taken = new ArrayList<>();
        for (Optional<WaitingAsks.Waiting> next = waiting.next(); next.isPresent(); next = waiting.next()) {
            taken.add(next.get().id());
        }
        return taken;
    }

    @Test
    void aNewerAskOfAKindThePointerAsksDropsTheOneWaiting() {
        WaitingAsks waiting = new WaitingAsks();
        assertTrue(waiting.add(ask(1, PcbAskKind.LEG)).isEmpty());
        assertEquals(Optional.of(1L), waiting.add(ask(2, PcbAskKind.LEG)).map(WaitingAsks.Waiting::id));
        assertEquals(List.of(2L), takenInTurn(waiting));
    }

    /**
     * Corners clicked faster than the engine lays them: on 2026-11-15 the leg to a corner clicked was a leg like the pointer's, and
     * the pointer's next move dropped it — of three corners clicked in a row, one or two were laid.
     */
    @Test
    void cornersClickedAreEveryOneAnsweredInTheOrderClickedWhateverThePointerAsksBetween() {
        WaitingAsks waiting = new WaitingAsks();
        assertTrue(waiting.add(ask(1, PcbAskKind.CLICKED_LEG)).isEmpty());
        assertTrue(waiting.add(ask(2, PcbAskKind.LEG)).isEmpty());
        assertTrue(waiting.add(ask(3, PcbAskKind.CLICKED_LEG)).isEmpty());
        assertEquals(Optional.of(2L), waiting.add(ask(4, PcbAskKind.LEG)).map(WaitingAsks.Waiting::id));
        assertTrue(waiting.add(ask(5, PcbAskKind.CLICKED_LEG)).isEmpty());
        assertEquals(List.of(1L, 3L, 5L, 4L), takenInTurn(waiting));
    }

    @Test
    void anAskAbandonedIsTakenOffAndAnotherIsNot() {
        WaitingAsks waiting = new WaitingAsks();
        waiting.add(ask(1, PcbAskKind.CLICKED_LEG));
        waiting.add(ask(2, PcbAskKind.CLICKED_LEG));
        assertTrue(waiting.abandon(1));
        assertFalse(waiting.abandon(7));
        assertEquals(List.of(2L), takenInTurn(waiting));
    }
}
