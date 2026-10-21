package org.jmouse.parallax.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.parallax.store.part.PartSearch;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 🔎 Parts looked for beyond the reader's own library — the catalogue, the KiCad libraries installed beside the server,
 * a supplier's search — and one found made the reader's own, into an owner's place.
 */
@RestController
@RequestMapping(ParallaxManagementRoutes.PREFIX)
public class PartSearchController {

    private final PartSearch parts;

    public PartSearchController(PartSearch parts) {
        this.parts = parts;
    }

    /**
     * @param provider the provider's code
     * @param identifier the candidate's identifier, as the search answered it
     */
    public record FetchRequest(String provider, String identifier) {
    }

    /** Every provider and whether it can answer now: configured, still reading its libraries, failing. */
    @GetMapping("/parts/providers")
    @PublicEndpoint("names the places parts are searched in and their state; holds nothing of anybody's")
    public List<PartSearch.ProviderResults> providers() {
        return parts.providers();
    }

    /** What every provider — or the one named — finds for the query, grouped by provider, each with its state. */
    @GetMapping("/parts/search")
    @PublicEndpoint("searches the published catalogue and outside sources; returns nothing a reader could not open")
    public List<PartSearch.ProviderResults> search(@RequestParam("q") String query,
                                                   @RequestParam(required = false) String provider) {
        return parts.search(query, provider);
    }

    /** A candidate stored as the owner's documents: its footprint, then the component landing on it. */
    @PostMapping(ParallaxManagementRoutes.OWNER + "/parts/fetch")
    @ResponseStatus(HttpStatus.CREATED)
    public PartSearch.Fetched fetch(@OwnerAddress String owner, @RequestBody FetchRequest request) {
        return parts.fetch(OwnerAddressArgumentResolver.ownerOf(owner), request == null ? null : request.provider(),
                request == null ? null : request.identifier());
    }
}
