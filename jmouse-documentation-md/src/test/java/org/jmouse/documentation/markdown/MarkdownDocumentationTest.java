package org.jmouse.documentation.markdown;

import org.jmouse.documentation.Documentation;
import org.jmouse.documentation.DocumentationPage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownDocumentationTest {

    private static final String PAGE = """
            ---
            subject: pcb-editor
            title: The PCB editor
            summary: Laying out a board
            ---
            ## Tools

            ### Track (W)
            Click from a pad.

            ```
            ## not a section
            ```
            ## What a shape becomes
            """;

    @Test
    void readsItsFrontMatterAndItsSections() {
        DocumentationPage page = MarkdownDocumentation.pageOf(PAGE, "pcb-editor.md");
        assertEquals("pcb-editor", page.subject());
        assertEquals("The PCB editor", page.title());
        assertEquals("Laying out a board", page.summary());
        assertEquals("markdown", page.format());
        assertEquals(List.of(new DocumentationPage.Section("Tools", "tools", 2), new DocumentationPage.Section("Track (W)", "track-w", 3),
                new DocumentationPage.Section("What a shape becomes", "what-a-shape-becomes", 2)), page.sections());
        assertTrue(page.text().startsWith("## Tools"), page.text());
    }

    @Test
    void refusesAPageThatSaysNotWhatItIs() {
        IllegalArgumentException none = assertThrows(IllegalArgumentException.class, () -> MarkdownDocumentation.pageOf("## Tools", "x.md"));
        assertTrue(none.getMessage().contains("front matter"), none.getMessage());
        IllegalArgumentException untitled = assertThrows(IllegalArgumentException.class,
                () -> MarkdownDocumentation.pageOf("---\nsubject: x\n---\ntext", "x.md"));
        assertTrue(untitled.getMessage().contains("title"), untitled.getMessage());
    }

    @Test
    void findsAPageByItsSubjectAndRefusesTwoOfOne() {
        DocumentationPage page = MarkdownDocumentation.pageOf(PAGE, "pcb-editor.md");
        Documentation documentation = new Documentation(List.of(() -> List.of(page)));
        assertEquals("The PCB editor", documentation.page("pcb-editor").orElseThrow().title());
        assertTrue(documentation.page("simulator").isEmpty());
        assertThrows(IllegalStateException.class, () -> new Documentation(List.of(() -> List.of(page), () -> List.of(page))));
    }
}
