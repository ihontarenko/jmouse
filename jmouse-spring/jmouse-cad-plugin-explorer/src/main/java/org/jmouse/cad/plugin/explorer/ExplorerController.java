package org.jmouse.cad.plugin.explorer;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.management.CadManagementRoutes;
import org.jmouse.cad.plugin.explorer.Explorer.EntryView;
import org.jmouse.cad.plugin.explorer.Explorer.MountView;
import org.jmouse.cad.plugin.explorer.Explorer.Opened;
import org.jmouse.storage.Content;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 📁 The explorer's routes — {@code JMOUSE_CAD_API.md} §2.10, under the plugin's own prefix. Every route decides inside its
 * service, through the engine's seam; a key is where a thing is from its mount's top, {@code docs/a.pdf}.
 */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX + "/plugins/" + ExplorerPlugin.ID)
public class ExplorerController {

    private final Explorer explorer;

    public ExplorerController(Explorer explorer) {
        this.explorer = explorer;
    }

    public record FolderRequest(String folder) {
    }

    public record MoveRequest(String from, String to) {
    }

    @GetMapping("/mounts")
    @PublicEndpoint("the mounts this reader may open, checked in the service")
    public List<MountView> mounts() {
        return explorer.mounts();
    }

    @GetMapping("/mounts/{mount}/entries")
    @PublicEndpoint("a folder's entries, for whoever may read the mount, checked in the service")
    public List<EntryView> entries(@PathVariable String mount, @RequestParam(required = false) String folder) {
        return explorer.entries(mount, folder);
    }

    /** A file's bytes, as its type says, under its own name. */
    @GetMapping("/mounts/{mount}/file")
    @PublicEndpoint("a file's bytes, for whoever may read the mount, checked in the service")
    public ResponseEntity<StreamingResponseBody> file(@PathVariable String mount, @RequestParam String key) {
        Opened opened = explorer.open(mount, key);
        String name = key.substring(key.lastIndexOf('/') + 1);
        StreamingResponseBody body = output -> {
            try (InputStream input = opened.resource().getInputStream()) {
                input.transferTo(output);
            }
        };
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(opened.description().contentType().toString()))
                .contentLength(opened.description().sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(name, StandardCharsets.UTF_8).build().toString())
                .body(body);
    }

    /** A file written from the request's body — made, or replaced. */
    @PutMapping("/mounts/{mount}/file")
    @PublicEndpoint("writes only where the reader may change the mount, checked in the service")
    public EntryView write(@PathVariable String mount, @RequestParam String key, HttpServletRequest request) {
        String name = key.substring(key.lastIndexOf('/') + 1);
        Content content = Content.of(name, request.getContentType(), request.getContentLengthLong(), request::getInputStream);
        return explorer.write(mount, key, content);
    }

    @PostMapping("/mounts/{mount}/folders")
    @ResponseStatus(HttpStatus.CREATED)
    @PublicEndpoint("writes only where the reader may change the mount, checked in the service")
    public void createFolder(@PathVariable String mount, @RequestBody FolderRequest request) {
        explorer.createFolder(mount, request.folder());
    }

    @PostMapping("/mounts/{mount}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicEndpoint("writes only where the reader may change the mount, checked in the service")
    public void move(@PathVariable String mount, @RequestBody MoveRequest request) {
        explorer.move(mount, request.from(), request.to());
    }

    @DeleteMapping("/mounts/{mount}/entries")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicEndpoint("writes only where the reader may change the mount, checked in the service")
    public void delete(@PathVariable String mount, @RequestParam String key, @RequestParam(defaultValue = "false") boolean folder) {
        explorer.delete(mount, key, folder);
    }
}
