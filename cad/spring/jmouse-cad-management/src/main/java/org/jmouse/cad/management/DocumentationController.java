package org.jmouse.cad.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.documentation.Documentation;
import org.jmouse.documentation.DocumentationPage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 📖 The engine's documentation: every page by its title, and one by the subject a window asks for it with. */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX + "/documentation")
public class DocumentationController {

    private final Documentation documentation;

    public DocumentationController(Documentation documentation) {
        this.documentation = documentation;
    }

    /** A page as a list names it: its subject, title and line, without its text. */
    public record PageEntry(String subject, String title, String summary) {
    }

    @GetMapping
    @PublicEndpoint("the documentation says how the application is used: anyone who may open it may read it")
    public List<PageEntry> pages() {
        return documentation.pages().stream().map(page -> new PageEntry(page.subject(), page.title(), page.summary())).toList();
    }

    @GetMapping("/{subject}")
    @PublicEndpoint("the documentation says how the application is used: anyone who may open it may read it")
    public DocumentationPage page(@PathVariable String subject) {
        return documentation.page(subject).orElseThrow(() -> CadProblem.notFound("The page of documentation on " + subject));
    }
}
