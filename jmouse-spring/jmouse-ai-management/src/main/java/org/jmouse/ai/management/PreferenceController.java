package org.jmouse.ai.management;

import org.jmouse.ai.preferences.AiPreferences;
import org.jmouse.ai.preferences.AiPreferences.Draft;
import org.jmouse.ai.preferences.AiPreferences.Setting;
import org.jmouse.ai.preferences.AiPreferences.StoredValue;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Reading and changing what this application tells its model — the system prompt, and whatever else a
 * product declares beside it.
 *
 * <p><strong>One controller for every setting rather than one per setting, because the shape is
 * identical.</strong> A setting is a declared name with several stored wordings, one of them in force;
 * a screen that can edit one can edit all of them, and a product adding a second setting adds a bean
 * rather than a route, a DTO and a form.
 *
 * <p><strong>The routes are the provider controller's, deliberately.</strong> {@code /{name}} is the
 * setting and {@code /values/{id}} is one wording of it, with {@code /in-force} as the press that
 * changes what the assistant reads — the same vocabulary, because it is the same operation on a
 * different table and a screen should not have to learn two.
 *
 * <p>⚠️ <strong>Carries no authorization annotation</strong>, exactly like the seven controllers beside
 * it. The gate is the product's — {@code ExternalAccessRules} is how a product declares a requirement
 * about a type it does not own — and a product that mounts this behind nothing has published the
 * ability to rewrite what its assistant is told to do. Which is worth being plain about: whoever can
 * write here decides what the model does with every permission every caller holds.
 */
@RestController
@RequestMapping(ManagementRoutes.PREFIX + "/preferences")
public class PreferenceController {

    private final AiPreferences preferences;

    public PreferenceController(AiPreferences preferences) {
        this.preferences = preferences;
    }

    /**
     * Every declared setting with every wording stored for it.
     *
     * <p>⚠️ Reading seeds. A setting with no rows at all is filled from what the product ships before
     * this answers, so the first person to open the screen finds the shipped wordings rather than an
     * empty table and a paragraph explaining why.
     */
    @GetMapping
    public List<Setting> settings() {
        return preferences.all();
    }

    @GetMapping("/{name}")
    public Setting setting(@PathVariable("name") String name) {
        return preferences.find(name);
    }

    /** A new wording, idle — putting it in force is a second request, and it is the one that lands. */
    @PostMapping("/{name}")
    public StoredValue add(@PathVariable("name") String name, @RequestBody Draft draft) {
        return preferences.add(name, draft);
    }

    @PutMapping("/values/{id}")
    public StoredValue change(@PathVariable("id") String id, @RequestBody Draft draft) {
        return preferences.change(id, draft);
    }

    /** ⚠️ Takes whatever was in force out of it — one operation, not two. */
    @PatchMapping("/values/{id}/in-force")
    public StoredValue putInForce(@PathVariable("id") String id) {
        return preferences.putInForce(id);
    }

    /** Back to the text this build ships for it. ⚠️ Refuses a wording nobody seeded. */
    @PostMapping("/values/{id}/shipped")
    public StoredValue restore(@PathVariable("id") String id) {
        return preferences.restore(id);
    }

    /** ⚠️ Refuses the one in force, so the assistant is never left with nothing to be told. */
    @DeleteMapping("/values/{id}")
    public ResponseEntity<Void> discard(@PathVariable("id") String id) {
        preferences.discard(id);

        return ResponseEntity.noContent().build();
    }
}
