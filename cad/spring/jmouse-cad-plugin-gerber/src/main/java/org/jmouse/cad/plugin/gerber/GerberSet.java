package org.jmouse.cad.plugin.gerber;

import org.jmouse.cad.board.drawing.BoardDrawing;
import org.jmouse.cad.board.drawing.FootprintDrawing;
import org.jmouse.cad.board.geometry.CopperNets;
import org.jmouse.cad.board.geometry.PadOutline;
import org.jmouse.cad.board.geometry.PlacedDesignator;
import org.jmouse.cad.board.geometry.Teardrops;
import org.jmouse.cad.board.geometry.ZonePours;
import org.jmouse.cad.library.CadStandards;
import org.jmouse.cad.exchange.ExportedFile;
import org.jmouse.gerber.GerberShape;
import org.jmouse.gerber.compose.GerberComposer;
import org.jmouse.strokefont.StrokeFont;
import org.locationtech.jts.geom.Geometry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The files a board house makes a board from, named as KiCad names them: a Gerber X2 file for each copper layer, the
 * solder mask, the paste and the silkscreen of each side, the edge, the plated and the unplated holes as Excellon, the assembly tables by
 * a profile, the netlist the bare board is tested against, and a README. Millimetres, the board's origin, Gerber's Y up.
 */
public final class GerberSet {

    /** What the fabrication's numbers are: the lines and openings no document carries. */
    public record Settings(double maskExpansion, double silkscreenWidth, double edgeWidth, double letterStroke, int curveSteps,
                           int cornerSteps, PlacedDesignator.Rules designator) {
    }

    /** What a file of the set is, by how its name ends, for the README. */
    public record FileRole(String ending, String role) {
    }

    /** A side of the board as its files name it. */
    private record Side(String id, String letter, String word) {
    }

    private static final List<Side> SIDES = List.of(new Side(BoardDrawing.TOP, "F", "Top"), new Side(BoardDrawing.BOTTOM, "B", "Bot"));

    private final Settings       settings;
    private final List<FileRole> roles;
    private final PadOutline     outlines;
    private final PadShapes      pads;
    private final CopperLayer    copper;
    private final Silkscreen     silkscreen;

    private GerberSet(Settings settings, List<FileRole> roles) {
        this.settings = settings;
        this.roles = List.copyOf(roles);
        this.outlines = new PadOutline(settings.curveSteps(), settings.cornerSteps());
        this.pads = new PadShapes(outlines);
        this.copper = new CopperLayer(pads, settings.curveSteps());
        this.silkscreen = new Silkscreen(StrokeFont.newStroke(), settings.designator(), settings.silkscreenWidth(),
                settings.letterStroke(), settings.curveSteps());
    }

    /** The set as this plugin ships it: its numbers and its README's words from the jar. */
    public static GerberSet shipped() {
        return new GerberSet(PluginResources.read(GerberSet.class, "fabrication.json", Settings.class),
                List.of(PluginResources.read(GerberSet.class, "files.json", FileRole[].class)));
    }

    /**
     * Every file of the set, in the order a reader expects them: copper from the top down, each side's mask and paste,
     * the edge, the drills, the assembly tables written by {@code profile}, the electrical test's netlist, the README.
     *
     * @param remarks what the README says beside the files: what the set does not carry yet
     */
    public List<ExportedFile> write(BoardFabrication fabrication, AssemblyProfile profile, List<String> remarks) {
        BoardDrawing board = fabrication.board();
        CopperNets nets = fabrication.copperNets(outlines, settings.curveSteps());
        List<Optional<Geometry>> pours = new ZonePours(board, nets, CadStandards.boardRules().valuesOf(board.rules()),
                settings.curveSteps()).pour();
        List<Teardrops.Drop> drops = Teardrops.of(board, nets.pads(), settings.curveSteps());
        List<ExportedFile> files = new ArrayList<>();
        List<String> stack = board.copperLayerIds();
        for (int position = 0; position < stack.size(); position++) {
            String layer = stack.get(position);
            files.add(new ExportedFile(named(fabrication, copperName(layer, position)),
                    copper.write(fabrication, layer, position, pours, drops)));
        }
        for (Side side : SIDES) {
            if (stack.contains(side.id())) {
                files.add(new ExportedFile(named(fabrication, side.letter() + "_Mask.gbr"), mask(fabrication, side)));
                files.add(new ExportedFile(named(fabrication, side.letter() + "_Paste.gbr"), paste(fabrication, side)));
                files.add(new ExportedFile(named(fabrication, side.letter() + "_Silkscreen.gbr"),
                        silkscreen.write(fabrication, side.id(), side.word())));
            }
        }
        files.add(new ExportedFile(named(fabrication, "Edge_Cuts.gbr"), edge(board)));
        Drills.Programs drills = Drills.of(fabrication);
        files.add(new ExportedFile(named(fabrication, "PTH.drl"), drills.plated()));
        files.add(new ExportedFile(named(fabrication, "NPTH.drl"), drills.unplated()));
        files.addAll(AssemblyTables.of(fabrication, profile));
        files.add(new ExportedFile(named(fabrication, "netlist.ipc"),
                TestNetlist.of(fabrication, nets)));
        files.add(new ExportedFile("README.txt", readme(fabrication, profile, files, remarks)));
        return files;
    }

    private static String named(BoardFabrication fabrication, String suffix) {
        return fabrication.name() + "-" + suffix;
    }

    /** KiCad's name for a copper layer's file: F_Cu, In1_Cu …, B_Cu. */
    private static String copperName(String layer, int position) {
        return switch (layer) {
            case BoardDrawing.TOP -> "F_Cu.gbr";
            case BoardDrawing.BOTTOM -> "B_Cu.gbr";
            default -> "In" + position + "_Cu.gbr";
        };
    }

    /**
     * A side's solder mask, negative — what is drawn is left bare —: every pad with copper on the side but an unplated
     * hole, and every plated mounting hole's ring, each grown by the mask's expansion. Vias stay covered.
     */
    private String mask(BoardFabrication fabrication, Side side) {
        GerberComposer mask = BoardGerber.file("Soldermask," + side.word(), false);
        openings(mask, fabrication, side, settings.maskExpansion(), pad -> !pad.isUnplated());
        for (BoardDrawing.Hole hole : fabrication.board().holes()) {
            if (hole.ring() > 0) {
                mask.flash(mask.aperture(new GerberShape.Circle(hole.ring() + settings.maskExpansion() * 2), null),
                           BoardGerber.out(hole.x(), hole.y()));
            }
        }
        return mask.document().toSource();
    }

    /** A side's paste stencil: every surface pad on the side, as it is. */
    private String paste(BoardFabrication fabrication, Side side) {
        GerberComposer paste = BoardGerber.file("Paste," + side.word(), true);
        openings(paste, fabrication, side, 0, FootprintDrawing.Pad::isSurface);
        return paste.document().toSource();
    }

    private void openings(GerberComposer composer, BoardFabrication fabrication, Side side, double growth,
                          Predicate<FootprintDrawing.Pad> taken) {
        for (BoardFabrication.SeatedPart part : fabrication.parts()) {
            for (FootprintDrawing.Pad pad : part.drawing().map(FootprintDrawing::pads).orElse(List.of())) {
                if (pad.hasCopperOn(side.id(), part.placement()) && taken.test(pad)) {
                    pads.draw(composer, part.placement(), pad, growth, null);
                }
            }
        }
    }

    /** The edge: every run of the outline drawn with a thin round pen. */
    private String edge(BoardDrawing board) {
        GerberComposer edge = BoardGerber.file("Profile,NP", true);
        int pen = edge.aperture(new GerberShape.Circle(settings.edgeWidth()), "Profile");
        for (List<double[]> run : BoardGerber.runs(board.outline(), settings.curveSteps())) {
            edge.line(pen, run.stream().map(point -> BoardGerber.out(point[0], point[1])).toList());
        }
        return edge.document().toSource();
    }

    /** What the set is, for whoever orders the board. */
    private String readme(BoardFabrication fabrication, AssemblyProfile profile, List<ExportedFile> files, List<String> remarks) {
        BoardDrawing board = fabrication.board();
        double[] extent = extentOf(BoardGerber.runs(board.outline(), settings.curveSteps()));
        List<String> lines = new ArrayList<>();
        lines.add(fabrication.name() + " — fabrication files");
        lines.add("");
        lines.add(String.format(Locale.ROOT, "Board: %.2f × %.2f mm, %d copper layer%s.", extent[0], extent[1],
                board.copperLayerIds().size(), board.copperLayerIds().size() == 1 ? "" : "s"));
        lines.add("Rules: " + board.rules().map(BoardDrawing.Rules::profile).orElse("standard") + ".");
        lines.add("Units: millimetres. Gerber X2, absolute coordinates; Excellon drills in decimal millimetres.");
        lines.add("Coordinates: the board's origin, Y upwards.");
        lines.addAll(remarks);
        lines.add("");
        lines.add("Files:");
        for (ExportedFile file : files) {
            lines.add(String.format(Locale.ROOT, "  %-32s %s", file.fileName(), roleOf(file.fileName(), profile)));
        }
        lines.add(String.format(Locale.ROOT, "  %-32s %s", "README.txt", "this file"));
        lines.add("");
        return String.join("\n", lines);
    }

    /** How wide and how tall the runs reach; nothing drawn is nothing wide. */
    private static double[] extentOf(List<List<double[]>> runs) {
        double left = Double.POSITIVE_INFINITY;
        double right = Double.NEGATIVE_INFINITY;
        double top = Double.POSITIVE_INFINITY;
        double bottom = Double.NEGATIVE_INFINITY;
        for (List<double[]> run : runs) {
            for (double[] point : run) {
                left = Math.min(left, point[0]);
                right = Math.max(right, point[0]);
                top = Math.min(top, point[1]);
                bottom = Math.max(bottom, point[1]);
            }
        }
        return right >= left ? new double[] {right - left, bottom - top} : new double[] {0, 0};
    }

    private String roleOf(String fileName, AssemblyProfile profile) {
        for (AssemblyProfile.Table table : List.of(profile.placement(), profile.materials())) {
            if (fileName.endsWith("-" + table.file())) {
                return table.role();
            }
        }
        return roles.stream().filter(role -> fileName.endsWith(role.ending())).map(FileRole::role).findFirst().orElse("");
    }
}
