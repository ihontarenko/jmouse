package org.jmouse.cad.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.store.parts.PartSearch;
import org.jmouse.cad.store.parts.PartSearch.Fetched;
import org.jmouse.cad.store.parts.PartSearch.ProviderResults;
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
 * 🔎 Parts from suppliers beside jmCAD's API — {@code JMOUSE_CAD_API.md} §2.7. Searching keeps nothing and reads only
 * the published catalogue and what the providers answer; a fetch writes only where the reader may write.
 */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX)
public class PartsController {

    private final PartSearch search;

    public PartsController(PartSearch search) {
        this.search = search;
    }

    /** Which part a reader keeps, from which provider. */
    public record FetchRequest(String provider, String identifier) {
    }

    @GetMapping("/parts/providers")
    @PublicEndpoint("which places parts are looked for in, and whether each answers; nothing stored is read")
    public List<ProviderResults> providers() {
        return search.providers();
    }

    @GetMapping("/parts/search")
    @PublicEndpoint("the published catalogue and what the providers answer; nothing is kept")
    public List<ProviderResults> search(@RequestParam("q") String query, @RequestParam(required = false) String provider) {
        return search.search(query, provider);
    }

    @PostMapping(CadManagementRoutes.OWNER + "/parts/fetch")
    @ResponseStatus(HttpStatus.CREATED)
    @PublicEndpoint("writes only where the reader may write the owner's documents, checked in the service")
    public Fetched fetch(@OwnerAddress String owner, @RequestBody FetchRequest request) {
        return search.fetch(owner, request.provider(), request.identifier());
    }
}
