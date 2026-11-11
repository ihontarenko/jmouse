package org.jmouse.cad.kicad;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaLineNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.simulation.SimulationLines;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.simulation.SimulationVocabulary;
import org.jmouse.el.grammar.ClauseBlockNode;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.ConnectionsNode;
import org.jmouse.el.grammar.LineBuilder;
import org.jmouse.el.grammar.LineGrammar;
import org.jmouse.el.grammar.Nodes;
import org.jmouse.el.grammar.SettingsNode;
import org.jmouse.el.node.expression.literal.QuantityLiteralNode;
import org.jmouse.units.Quantity;

import java.util.ArrayList;
import java.util.List;

/**
 * An imported sheet ({@link ImportedSheet}) as its circuit's document, built as its tree: what it hands across its edge,
 * its parts, where each is drawn and the wires between them, the probes it opens on, and the transient it runs.
 */
public final class KiCadCircuitDocument {

    /** How a sheet's port goes: an imported one says nothing of its direction. */
    private static final String PORT_DIRECTION = SimulationVocabulary.ANALOG;

    /** The unit an oscilloscope's channel is scaled in. */
    private static final String VOLTS_A_DIVISION = "V/div";

    private KiCadCircuitDocument() {
    }

    /**
     * @param summary what the circuit says of itself: where it was imported from
     */
    public static DocumentNode of(String slug, String title, String summary, ImportedSheet sheet, KiCadLibraries libraries) {
        DocumentNode document = new DocumentNode(SimulationModule.CIRCUIT, title, slug, true, null, 1);
        MetaNode meta = new MetaNode();
        meta.addExpression(MetaLineNode.of(MetaNode.SUMMARY, Nodes.text(summary)));
        meta.addExpression(MetaLineNode.of(MetaNode.CATEGORY, Nodes.word(libraries.category())));
        meta.addExpression(MetaLineNode.of(MetaNode.TAGS, Nodes.words(libraries.circuitTags())));
        document.addExpression(meta);
        LineGrammar port = SimulationLines.PORTS.stream().filter(line -> line.word().equals(PORT_DIRECTION)).findFirst()
                .orElseThrow();
        addBlock(document, "ports", sheet.ports().stream()
                .map(net -> LineBuilder.line(port).slot("net", Nodes.word(net)).build()).toList());
        addBlock(document, "nets", sheet.globals().stream()
                .map(net -> LineBuilder.line(SimulationLines.NET).slot("name", Nodes.name(net)).flag("global").build()).toList());
        addBlock(document, "parts", sheet.parts().stream().map(KiCadCircuitDocument::part).toList());
        List<ClauseLineNode> layout = new ArrayList<>();
        sheet.items().forEach(item -> layout.add(item(item)));
        sheet.wires().forEach(wire -> layout.add(wire(wire)));
        addBlock(document, "layout", layout);
        addBlock(document, "probes", sheet.probes().stream().map(KiCadCircuitDocument::probe).toList());
        document.addExpression(new ClauseBlockNode("analysis", LineBuilder.line(SimulationLines.ANALYSIS)
                .slot("kind", Nodes.word(SimulationVocabulary.TRANSIENT))
                .clause("for", new QuantityLiteralNode(Quantity.prefixed(libraries.span(), "s"))).build()));
        return document;
    }

    /** {@code R1 resistor (a = in, b = out) resistance = 1000 [symbol slug] [footprint slug]}. */
    private static ClauseLineNode part(ImportedSheet.Part part) {
        List<ConnectionsNode.Connection> connections = new ArrayList<>();
        part.pins().forEach((pin, net) -> connections.add(new ConnectionsNode.Connection(pin, net)));
        LineBuilder line = LineBuilder.line(SimulationLines.PART).slot("reference", Nodes.name(part.id()))
                .slot("component", Nodes.word(part.component())).slot("connections", new ConnectionsNode(connections));
        if (!part.overrides().isEmpty()) {
            List<SettingsNode.Setting> settings = new ArrayList<>();
            part.overrides().forEach((name, value) -> settings.add(new SettingsNode.Setting(name,
                    Nodes.formula(Nodes.number(Double.parseDouble(value))))));
            line.slot("settings", new SettingsNode(settings));
        }
        if (part.symbol() != null) {
            line.clause("symbol", Nodes.word(part.symbol()));
        }
        if (part.footprint() != null) {
            line.clause("footprint", Nodes.word(part.footprint()));
        }
        return line.build();
    }

    /** {@code R1 at x, y [rotate r] [mirror] [size w x h]}, {@code U4.B at …}, {@code GND1 ground [net] at x, y [rotate r]}. */
    private static ClauseLineNode item(ImportedSheet.Item item) {
        if (item.mark() != null) {
            LineBuilder mark = LineBuilder.line(SimulationLines.MARK).slot("reference", Nodes.name(item.id()))
                    .slot("symbol", Nodes.word(item.mark()));
            if (item.net() != null) {
                mark.slot("net", Nodes.word(item.net()));
            }
            mark.clause("at", Nodes.point(item.x(), item.y()));
            if (item.rotation() != 0) {
                mark.clause("rotate", Nodes.number(item.rotation()));
            }
            return mark.build();
        }
        String reference = item.unit() == null ? item.id() : item.id() + "." + item.unit();
        LineBuilder place = LineBuilder.line(SimulationLines.PLACE).slot("reference", Nodes.name(reference))
                .clause("at", Nodes.point(item.x(), item.y()));
        if (item.rotation() != 0) {
            place.clause("rotate", Nodes.number(item.rotation()));
        }
        if (item.mirrored()) {
            place.flag("mirror");
        }
        if (item.size() != null) {
            place.clause("size", Nodes.extent(item.size()[0], item.size()[1]));
        }
        return place.build();
    }

    /** {@code W1 wire R1.b to C1.a [via x y, x y …]}. */
    private static ClauseLineNode wire(ImportedSheet.Wire wire) {
        LineBuilder line = LineBuilder.line(SimulationLines.WIRE).slot("reference", Nodes.name(wire.id()))
                .slot("from", Nodes.word(wire.from())).clause("to", Nodes.word(wire.to()));
        if (!wire.bends().isEmpty()) {
            line.clause("via", Nodes.groups(wire.bends().stream()
                    .map(bend -> List.of(Quantity.plain(bend[0]).written(), Quantity.plain(bend[1]).written())).toList()));
        }
        return line.build();
    }

    /** {@code CH1 vcc 20 V/div}. */
    private static ClauseLineNode probe(ImportedSheet.Probe probe) {
        return LineBuilder.line(SimulationLines.PROBE).slot("channel", Nodes.name(probe.channel()))
                .slot("subject", Nodes.word(probe.net()))
                .slot("scale", Nodes.formula(new QuantityLiteralNode(Quantity.prefixed(probe.scale(), VOLTS_A_DIVISION))))
                .build();
    }

    private static void addBlock(DocumentNode document, String word, List<ClauseLineNode> lines) {
        if (lines.isEmpty()) {
            return;
        }
        ClauseBlockNode block = new ClauseBlockNode(word, null);
        block.open();
        lines.forEach(block::addExpression);
        document.addExpression(block);
    }
}
