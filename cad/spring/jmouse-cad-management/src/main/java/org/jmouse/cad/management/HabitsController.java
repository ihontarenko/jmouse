package org.jmouse.cad.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.store.habits.CadHabits;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 🧭 The signed-in reader's habits — what the interface remembers of how they work —, the same in every product that embeds the
 * engine: read whole, changed a few at a time.
 */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX)
public class HabitsController {

    private final CadHabits habits;

    public HabitsController(CadHabits habits) {
        this.habits = habits;
    }

    /** Every habit of the reader: the installation's, each one they changed as they have it. */
    @GetMapping("/habits")
    @PublicEndpoint("decides inside its service: a reader's habits are their own, and nobody signed in has none")
    public Map<String, String> habits() {
        return habits.habits();
    }

    /** Changes some of the reader's habits, all or none; an empty value forgets the reader's own. */
    @PatchMapping("/habits")
    @PublicEndpoint("decides inside its service: a reader changes only their own habits")
    public Map<String, String> change(@RequestBody Map<String, String> changes) {
        return habits.change(changes);
    }
}
