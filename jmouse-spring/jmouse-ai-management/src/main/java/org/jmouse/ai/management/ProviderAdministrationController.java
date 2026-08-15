package org.jmouse.ai.management;

import org.jmouse.ai.administration.ProviderAdministration;
import org.jmouse.ai.administration.ProviderAdministration.Configuration;
import org.jmouse.ai.administration.ProviderAdministration.Draft;
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
 * Changing which model this application talks to, on whose key.
 *
 * <p><strong>The one controller in this module that writes, and it exists because the alternative was
 * every adopter writing it.</strong> The module used to say that changing the provider "belongs behind a
 * product's own authorization rather than behind whatever a library guessed" — which was right about the
 * <em>gate</em> and wrong about the <em>code</em>. Two products then wrote the same repository, the same
 * one-row-in-force rule, the same blank-key-means-keep rule and the same six routes, and the rules they
 * were re-deriving are {@code jmouse-ai-jpa}'s own. The gate is still the product's: this controller
 * carries no authorization annotation, exactly like the four beside it, and a product that mounts it
 * behind nothing has published its provider configuration.
 *
 * <p>⚠️ <strong>Present only where the application has something to administer.</strong> The port has an
 * {@link ProviderAdministration#unavailable()} form for settings that come from configuration, and every
 * write on it refuses with a sentence saying so.
 *
 * <p>⚠️ <strong>No response carries a key</strong>, and none can be made to: {@link Configuration} has
 * no field for one. A key travels in on a {@link Draft} and never comes back out.
 */
@RestController
@RequestMapping(ManagementRoutes.PREFIX + "/configurations")
public class ProviderAdministrationController {

    private final ProviderAdministration configurations;

    public ProviderAdministrationController(ProviderAdministration configurations) {
        this.configurations = configurations;
    }

    /**
     * What a new configuration may name, alongside what is stored.
     *
     * <p>The two travel together so a screen can offer a list rather than a text box, and so a provider
     * added to or removed from the library changes what is offerable without a deploy of the screen.
     *
     * @param supportedProviders every provider name a configuration may carry
     * @param configurations     what is stored, oldest first
     */
    public record StoredConfigurations(
            List<String>        supportedProviders,
            List<Configuration> configurations
    ) {
    }

    @GetMapping
    public StoredConfigurations configurations() {
        return new StoredConfigurations(
                configurations.supportedProviders(), configurations.configurations());
    }

    /** One by identifier, or 404 where this application has no such row. */
    @GetMapping("/{id}")
    public ResponseEntity<Configuration> configuration(@PathVariable("id") String id) {
        return configurations.find(id).map(ResponseEntity::ok).orElseGet(
                () -> ResponseEntity.notFound().build());
    }

    /** A new one, idle — putting it in force is a second request, and it is the one that spends money. */
    @PostMapping
    public Configuration add(@RequestBody Draft draft) {
        return configurations.add(draft);
    }

    /** ⚠️ A blank key on the draft keeps the stored one; it never clears it. */
    @PutMapping("/{id}")
    public Configuration change(@PathVariable("id") String id, @RequestBody Draft draft) {
        return configurations.change(id, draft);
    }

    @PatchMapping("/{id}/in-force")
    public Configuration putInForce(@PathVariable("id") String id) {
        return configurations.putInForce(id);
    }

    @DeleteMapping("/{id}/in-force")
    public Configuration takeOutOfForce(@PathVariable("id") String id) {
        return configurations.takeOutOfForce(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> discard(@PathVariable("id") String id) {
        configurations.discard(id);

        return ResponseEntity.noContent().build();
    }
}
