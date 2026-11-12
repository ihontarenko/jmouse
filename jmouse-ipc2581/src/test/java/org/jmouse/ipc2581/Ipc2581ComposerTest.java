package org.jmouse.ipc2581;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Ipc2581ComposerTest {

    /** The standard's own schema, revision C, which the classes are generated from and every file is checked against. */
    private static final File SCHEMA = new File("src/main/xsd/IPC-2581C.xsd");

    private static final Ipc2581Composer.Sender SENDER = new Ipc2581Composer.Sender("jmCAD", "jmCAD", "jmCAD", "jMouse", "1");

    @Test
    void writesAFileTheSchemaAcceptsWithEachShapeOnce() throws Exception {
        Ipc2581Shape pad = new Ipc2581Shape.RectCenter(1.2, 1.4);
        Ipc2581Composer composer = new Ipc2581Composer("board", "board", SENDER, OffsetDateTime.of(2026, 10, 30, 12, 0, 0, 0, ZoneOffset.UTC))
                .layer(new Ipc2581Composer.Layer("TOP", "CONDUCTOR", Ipc2581Composer.Side.TOP))
                .drillLayer("DRILL", "TOP", "TOP")
                .stackup(1.6, List.of(new Ipc2581Composer.StackupLayer("TOP", 0.035)))
                .profile(Ipc2581Polygon.of(List.of(new Ipc2581Point(0, 0), new Ipc2581Point(10, 0), new Ipc2581Point(10, 5))))
                .packageOf(new Ipc2581Composer.Package("r-0805", "1", Ipc2581Polygon.of(List.of(new Ipc2581Point(-1, -1),
                        new Ipc2581Point(1, -1), new Ipc2581Point(1, 1))), List.of(
                        new Ipc2581Composer.Pin("1", false, new Ipc2581Point(-1, 0), pad),
                        new Ipc2581Composer.Pin("2", false, new Ipc2581Point(1, 0), pad))))
                .component(new Ipc2581Composer.Component("R1", "r-0805", "resistor", "TOP", false, 90, false, new Ipc2581Point(5, 2.5)))
                .bomItem(new Ipc2581Composer.BomItem("resistor", "10 kΩ", 2, List.of("R1"), Map.of("value", "10 kΩ")))
                .netPin("GND", new Ipc2581Composer.PinReference("R1", "1"))
                .pad("TOP", "GND", new Ipc2581Composer.PlacedPad(pad, new Ipc2581Point(4, 2.5), new Ipc2581Composer.PinReference("R1", "1")))
                .graphics("TOP", null, List.of(new Ipc2581Graphic.Line(new Ipc2581Point(4, 2.5), new Ipc2581Point(8, 2.5), 0.25)))
                .hole("DRILL", null, "H1", 0.8, Ipc2581Composer.Plating.PLATED, new Ipc2581Point(8, 2.5));
        String written = composer.write();
        SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(SCHEMA).newValidator()
                .validate(new StreamSource(new StringReader(written)));
        DocumentBuilderFactory parsing = DocumentBuilderFactory.newInstance();
        parsing.setNamespaceAware(true);
        Document document = parsing.newDocumentBuilder().parse(new ByteArrayInputStream(written.getBytes(StandardCharsets.UTF_8)));
        Element root = document.getDocumentElement();
        assertEquals(List.of("Content", "LogisticHeader", "HistoryRecord", "Bom", "Ecad"), childNames(root));
        Element step = (Element) root.getElementsByTagNameNS("*", "Step").item(0);
        assertEquals(List.of("Datum", "Profile", "Package", "Component", "LogicalNet", "LayerFeature", "LayerFeature"), childNames(step));
        assertEquals(1, document.getElementsByTagNameNS("*", "EntryStandard").getLength(), "a shape used three times is written once");
        assertEquals("10 kΩ", ((Element) document.getElementsByTagNameNS("*", "BomItem").item(0)).getAttribute("description"));
    }

    private static List<String> childNames(Element element) {
        List<String> names = new ArrayList<>();
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element found) {
                names.add(found.getLocalName());
            }
        }
        return names;
    }
}
