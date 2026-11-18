package org.jmouse.cad.plugin.panel;

import org.jmouse.cad.Point;
import org.jmouse.cad.drawing.Arrangement;
import org.jmouse.cad.drawing.DrawnGroup;
import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.document.DocumentInput;
import org.jmouse.cad.document.DocumentLanguage;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.management.CadManagementRoutes;
import org.jmouse.cad.panel.PanelDrawing;
import org.jmouse.cad.panel.PanelModule;
import org.jmouse.cad.panel.PanelSeparation;
import org.jmouse.cad.panel.PanelSide;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * What the panel screen asks: a panel's text from its form and back, and a pcb's copper and edge as a picture draws them. Texts
 * the caller sent are all it reads, so it decides nothing about who may see what; storing a panel goes through the catalogue.
 */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX + "/plugins/" + PanelPlugin.ID)
public class PanelController {

    private final CadLanguage language;

    public PanelController(CadLanguage language) {
        this.language = language;
    }

    /** A copy of a board as a form holds it. */
    public record BoardForm(String pcb, double x, double y, double rotation, int across, int down, Double gap, Arrangement arrangement) {
    }

    /** A panel's frame as a form holds it. */
    public record FrameForm(Double rail, List<String> sides, Integer fiducials, Integer tooling) {
    }

    /** A point with what stands there, as a form holds it. */
    public record MarkForm(double x, double y, Double diameter, Double opening, Double drill) {
    }

    /** A panel as a form holds it. */
    public record PanelForm(List<BoardForm> boards, String separation, FrameForm frame, List<MarkForm> fiducials, List<MarkForm> tooling,
                            List<DrawnGroup> groups) {
    }

    /** @param source the panel's text so far, whose head is kept; null for a new one */
    public record WrittenRequest(String source, String slug, String title, String summary, PanelForm panel) {
    }

    public record SourceText(String source) {
    }

    @PostMapping("/written")
    @PublicEndpoint("a text the caller sent; nothing stored is read")
    public SourceText written(@RequestBody WrittenRequest request) {
        DocumentNode document = new DocumentNode(PanelModule.PANEL, request.title(), request.slug(), null, 1);
        if (request.source() == null || request.source().isBlank()) {
            // A new panel says what it is and nothing more: its author files it, when it is filed at all.
            document.addExpression(MetaNode.filed(request.summary() == null ? "" : request.summary(), null, null, List.of()));
        } else {
            read(request.source()).getExpressions().stream().filter(MetaNode.class::isInstance).forEach(document::addExpression);
        }
        drawingOf(request.panel()).lines().forEach(document::addExpression);
        return new SourceText(language.language().write(document));
    }

    @PostMapping("/form")
    @PublicEndpoint("a text the caller sent; nothing stored is read")
    public PanelForm form(@RequestBody SourceText text) {
        PanelDrawing panel = PanelDrawing.of(read(text.source()));
        return new PanelForm(panel.boards().stream().map(board -> new BoardForm(board.pcb(), board.x(), board.y(), board.rotation(), board.across(),
                board.down(), value(board.gap()), board.arrangement())).toList(), panel.separation().word(), panel.frame().map(frame -> new FrameForm(value(frame.rail()),
                frame.sides().stream().map(PanelSide::word).toList(), value(frame.fiducials()), value(frame.tooling()))).orElse(null),
                panel.fiducials().stream().map(fiducial -> new MarkForm(fiducial.at().x(), fiducial.at().y(), value(fiducial.diameter()), value(fiducial.opening()), null)).toList(),
                panel.tooling().stream().map(tooling -> new MarkForm(tooling.at().x(), tooling.at().y(), null, null, value(tooling.drill()))).toList(),
                panel.groups());
    }

    /** What a picture of a pcb draws: its edge, its copper's runs, its holes and its drawn shapes. */
    public record Picture(String outline, List<Run> runs, List<Round> rounds, List<PictureShape> shapes) {
    }

    public record Run(String layer, double width, List<Point> points) {
    }

    public record Round(double x, double y, double diameter) {
    }

    public record PictureShape(String layer, double width, boolean filled, String path) {
    }

    @PostMapping("/picture")
    @PublicEndpoint("a text the caller sent; nothing stored is read")
    public Picture picture(@RequestBody SourceText text) {
        PcbDrawing pcb = PcbDrawing.of(read(text.source()));
        List<Run> runs = new ArrayList<>();
        pcb.tracks().forEach(track -> runs.add(new Run(track.layer(), track.width(), track.points())));
        pcb.arcs().forEach(arc -> runs.add(new Run(arc.layer(), arc.width(), List.of(arc.from(), arc.through(), arc.to()))));
        List<Round> rounds = new ArrayList<>();
        pcb.vias().forEach(via -> rounds.add(new Round(via.x(), via.y(), via.drill())));
        pcb.holes().forEach(hole -> rounds.add(new Round(hole.x(), hole.y(), hole.drill())));
        return new Picture(pcb.outline(), runs, rounds, pcb.shapes().stream()
                .map(shape -> new PictureShape(shape.layer(), shape.width(), shape.filled(), shape.path())).toList());
    }

    private DocumentNode read(String source) {
        DocumentLanguage reading = language.language();
        return reading.readOne(DocumentInput.text("the document", source));
    }

    private static PanelDrawing drawingOf(PanelForm form) {
        List<PanelDrawing.Board> boards = form.boards().stream().map(board -> new PanelDrawing.Board(board.pcb(), board.x(), board.y(), board.rotation(),
                board.across(), board.down(), optional(board.gap()), board.arrangement())).toList();
        Optional<PanelDrawing.Frame> frame = Optional.ofNullable(form.frame()).map(given -> new PanelDrawing.Frame(optional(given.rail()),
                PanelSide.ofWords(given.sides() == null ? List.of() : given.sides()), optional(given.fiducials()), optional(given.tooling())));
        List<PanelDrawing.Fiducial> fiducials = (form.fiducials() == null ? List.<MarkForm>of() : form.fiducials()).stream().map(mark -> new PanelDrawing.Fiducial(
                new Point(mark.x(), mark.y()), optional(mark.diameter()), optional(mark.opening()))).toList();
        List<PanelDrawing.Tooling> tooling = (form.tooling() == null ? List.<MarkForm>of() : form.tooling()).stream().map(mark -> new PanelDrawing.Tooling(
                new Point(mark.x(), mark.y()), optional(mark.drill()))).toList();
        return new PanelDrawing(boards, PanelSeparation.ofWord(form.separation()), frame, fiducials, tooling, form.groups());
    }

    private static OptionalDouble optional(Double value) {
        return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    private static OptionalInt optional(Integer value) {
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }

    private static Double value(OptionalDouble value) {
        return value.isPresent() ? value.getAsDouble() : null;
    }

    private static Integer value(OptionalInt value) {
        return value.isPresent() ? value.getAsInt() : null;
    }
}
