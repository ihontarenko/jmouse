package org.jmouse.ai.management;

import org.jmouse.ai.view.ProviderRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Which model provider is in force, and whether it can authenticate.
 *
 * <p>⚠️ <strong>No endpoint here can return a key, and none can be added that would.</strong> The port
 * behind it has no method that answers with one — the credential is reduced to a boolean at the boundary
 * where the settings are read, long before anything HTTP-shaped sees them. That is not caution about a
 * badly written screen; it is the only shape in which "read the provider configuration" is safe to
 * expose at all, given that the answer would otherwise travel through an access log, a browser cache and
 * whatever proxies sit in between.
 *
 * <p><strong>And nothing here writes.</strong> Changing which provider is in force changes what this
 * application sends somebody else's servers and what it is billed for, which is a decision that belongs
 * behind a product's own authorization rather than behind whatever a library guessed.
 */
@RestController
@RequestMapping(ManagementRoutes.PREFIX)
public class ProviderController {

    private final ProviderRegistry providers;

    public ProviderController(ProviderRegistry providers) {
        this.providers = providers;
    }

    /**
     * The active provider, or 204 where none is configured.
     *
     * <p>No content rather than a 404: nothing is missing, there is simply nothing configured, and a
     * screen distinguishing "you asked about something that does not exist" from "this application has
     * no provider" is a screen that can tell somebody what to do next.
     */
    @GetMapping("/provider")
    public ResponseEntity<ProviderRegistry.ActiveProvider> provider() {
        return providers.active()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
