package org.jmouse.cad.management;

import org.jmouse.cad.store.catalogue.WrittenOwners;
import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.exchange.ExchangeFiles;
import org.jmouse.cad.exchange.ExportedFiles;
import org.jmouse.cad.store.exchange.DocumentExchange;
import org.jmouse.cad.store.exchange.DocumentExchange.Format;
import org.jmouse.cad.store.exchange.DocumentExchange.Imported;
import org.jmouse.cad.store.exchange.DocumentExchange.Read;
import org.jmouse.cad.store.problem.CadProblem;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 🔁 Documents to and from other formats — {@code JMOUSE_CAD_API.md} §2.6. Which formats there are is the plugins'; the
 * routes are the same for every one, so an interface needs to know none of them by name.
 */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX)
public class ExchangeController {

    private final DocumentExchange exchange;

    public ExchangeController(DocumentExchange exchange) {
        this.exchange = exchange;
    }

    /**
     * Files the reader chose.
     *
     * @param files every file by its path, the one read first among them
     * @param main  the file read first
     * @param title what the made document is called, or null for the file's own word
     * @param owner whose components a converted part may also name, as {@code TYPE:id}; null for the catalogue's alone
     */
    public record FilesRequest(Map<String, String> files, String main, String title, String owner) {

        private ExchangeFiles exchangeFiles() {
            try {
                return new ExchangeFiles(files, main, title);
            } catch (IllegalArgumentException refused) {
                throw CadProblem.invalid(refused.getMessage());
            }
        }
    }

    /** @param owner whose components the document's parts may also name, as {@code TYPE:id}; null for the catalogue's */
    public record ExportRequest(String source, String owner) {
    }

    @GetMapping("/exchange")
    @PublicEndpoint("the formats this server converts; nothing stored is read")
    public List<Format> formats() {
        return exchange.formats();
    }

    @PostMapping("/exchange/{format}/read")
    @PublicEndpoint("files the caller sent, converted and handed back; components are read as the reader may see them")
    public Read read(@PathVariable String format, @RequestBody FilesRequest request) {
        return exchange.read(format, request.exchangeFiles(), WrittenOwners.optional(request.owner()));
    }

    @PostMapping(CadManagementRoutes.OWNER + "/exchange/{format}/import")
    @ResponseStatus(HttpStatus.CREATED)
    @PublicEndpoint("writes only where the reader may write the owner's documents, checked in the service")
    public Imported importInto(@OwnerAddress String owner, @PathVariable String format, @RequestBody FilesRequest request) {
        return exchange.importInto(OwnerAddressArgumentResolver.ownerOf(owner), format, request.exchangeFiles());
    }

    @PostMapping("/exchange/{format}/export")
    @PublicEndpoint("a text the caller sent; components are read as the reader may see them")
    public ExportedFiles export(@PathVariable String format, @RequestBody ExportRequest request) {
        return exchange.export(format, request.source(), WrittenOwners.optional(request.owner()));
    }
}
