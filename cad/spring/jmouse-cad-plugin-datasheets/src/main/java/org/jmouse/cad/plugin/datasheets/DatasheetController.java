package org.jmouse.cad.plugin.datasheets;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.management.CadManagementRoutes;
import org.jmouse.cad.store.catalogue.WrittenOwners;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** 📄 A document's datasheets, under the plugin's own prefix. Every route decides inside the service. */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX + "/plugins/" + DatasheetPlugin.ID + "/documents/{address}")
public class DatasheetController {

    private final Datasheets datasheets;

    public DatasheetController(Datasheets datasheets) {
        this.datasheets = datasheets;
    }

    @GetMapping
    @PublicEndpoint("answered only when the reader may see the document, checked in the service")
    public Datasheets.DatasheetShelf shelf(@PathVariable String address, @RequestParam(required = false) String owner) {
        return datasheets.shelf(address, WrittenOwners.optional(owner));
    }

    @GetMapping("/file")
    @PublicEndpoint("answered only when the reader may see the document, checked in the service")
    public ResponseEntity<byte[]> open(@PathVariable String address, @RequestParam(required = false) String owner, @RequestParam String name) {
        Datasheets.OpenedDatasheet opened = datasheets.open(address, WrittenOwners.optional(owner), name);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(opened.mediaType()))
                .cacheControl(CacheControl.noCache().cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(opened.name(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(opened.content());
    }

    @PutMapping("/file")
    @PublicEndpoint("kept only where the reader may file beside the document, checked in the service")
    public Datasheets.DatasheetShelf keep(@PathVariable String address, @RequestParam(required = false) String owner, @RequestParam String name, HttpServletRequest request)
            throws IOException {
        return datasheets.keep(address, WrittenOwners.optional(owner), name, request.getInputStream());
    }

    @DeleteMapping("/file")
    @PublicEndpoint("taken away only where the reader may file beside the document, checked in the service")
    public Datasheets.DatasheetShelf remove(@PathVariable String address, @RequestParam(required = false) String owner, @RequestParam String name) {
        return datasheets.remove(address, WrittenOwners.optional(owner), name);
    }
}
