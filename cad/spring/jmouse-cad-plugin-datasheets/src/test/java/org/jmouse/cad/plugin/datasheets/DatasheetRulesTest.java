package org.jmouse.cad.plugin.datasheets;

import org.jmouse.cad.settings.CadSettings;
import org.jmouse.cad.document.MetaLineNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.el.grammar.Nodes;
import org.jmouse.el.grammar.ListNode;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatasheetRulesTest {

    private final DatasheetFormat format = CadSettings.shipped().read(DatasheetPlugin.FORMAT);

    @Test
    void onlyADatasheetsOwnKindCounts() {
        assertTrue(format.names("tl072.pdf"));
        assertTrue(format.names("TL072.PDF"));
        assertFalse(format.names("soic-8.step"));
        assertFalse(format.names("pdf"));
    }

    @Test
    void aNameIsAFilesNameAlone() {
        assertEquals("tl072 rev F.pdf", format.requireName("tl072 rev F.pdf"));
        assertThrows(CadProblem.class, () -> format.requireName("../../etc/tl072.pdf"));
        assertThrows(CadProblem.class, () -> format.requireName("folder\\tl072.pdf"));
        assertThrows(CadProblem.class, () -> format.requireName("tl072\r\n.pdf"));
        assertThrows(CadProblem.class, () -> format.requireName("tl072.html"));
        assertThrows(CadProblem.class, () -> format.requireName(" "));
        assertThrows(CadProblem.class, () -> format.requireName(null));
    }

    @Test
    void aFileOnlyCalledAPdfIsRefused() {
        assertDoesNotThrow(() -> format.requireContent("a.pdf", "%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII)));
        assertThrows(CadProblem.class, () -> format.requireContent("a.pdf", "<html>".getBytes(StandardCharsets.US_ASCII)));
        assertThrows(CadProblem.class, () -> format.requireContent("a.pdf", new byte[0]));
    }

    /** The links are read back as a program writes them: the importer of KiCad's parts is who writes them today. */
    @Test
    void linksAreReadAsTheyAreWritten() {
        MetaNode meta = new MetaNode();
        meta.addExpression(MetaLineNode.group(MetaNode.LINKS, List.of(
                new ListNode(List.of(Nodes.text("Datasheet"), Nodes.text("https://www.ti.com/lit/ds/symlink/tl072.pdf")), false),
                new ListNode(List.of(Nodes.text("KiCad libraries"), Nodes.text("https://www.kicad.org/libraries")), false))));
        assertEquals(List.of(new MetaNode.Link("Datasheet", "https://www.ti.com/lit/ds/symlink/tl072.pdf"),
                             new MetaNode.Link("KiCad libraries", "https://www.kicad.org/libraries")), meta.links());
    }
}
