package org.jmouse.cad.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.store.files.DocumentUnderlays;
import org.jmouse.cad.store.files.DocumentUnderlays.Placement;
import org.jmouse.cad.store.files.DocumentUnderlays.Underlay;
import org.jmouse.cad.store.problem.CadProblem;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 🖼️ The picture a document's drawing is drawn over: kept with the document. Every route decides inside the service. */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX + "/documents/{address}/underlay")
public class UnderlaysController {

    private final DocumentUnderlays underlays;

    public UnderlaysController(DocumentUnderlays underlays) {
        this.underlays = underlays;
    }

    /** The picture under the drawing and where it lies; no content where it has none. */
    @GetMapping
    @PublicEndpoint("answered only when the reader may see the document, checked in the service")
    public ResponseEntity<Underlay> underlay(@PathVariable String address) {
        return underlays.of(address).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/picture")
    @PublicEndpoint("answered only when the reader may see the document, checked in the service")
    public ResponseEntity<byte[]> picture(@PathVariable String address) {
        DocumentUnderlays.Picture picture = underlays.picture(address);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(picture.format().mediaType()))
                .cacheControl(CacheControl.noCache().cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(picture.content());
    }

    /** A picture laid under the drawing, its bytes the body, where it lies the query: in place of the one it had. */
    @PutMapping("/picture")
    @PublicEndpoint("laid only where the reader may file beside the document, checked in the service")
    public Underlay lay(@PathVariable String address, @RequestHeader(HttpHeaders.CONTENT_TYPE) String mediaType, @RequestParam String name,
                        @RequestParam double x, @RequestParam double y, @RequestParam double width, @RequestParam double aspect,
                        @RequestParam double opacity, @RequestParam boolean shown, @RequestBody byte[] content) {
        return underlays.lay(address, name, mediaType, content, new Placement(x, y, width, aspect, opacity, shown));
    }

    /** The picture moved, made fainter or stronger, shown or hidden. */
    @PutMapping
    @PublicEndpoint("placed only where the reader may file beside the document, checked in the service")
    public Underlay place(@PathVariable String address, @RequestBody Placement placement) {
        if (placement == null) {
            throw CadProblem.invalid("Say where the picture lies.");
        }
        return underlays.place(address, placement);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicEndpoint("taken away only where the reader may file beside the document, checked in the service")
    public void remove(@PathVariable String address) {
        underlays.remove(address);
    }
}
