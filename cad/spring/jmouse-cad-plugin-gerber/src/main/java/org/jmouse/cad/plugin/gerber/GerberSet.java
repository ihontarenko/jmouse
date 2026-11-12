package org.jmouse.cad.plugin.gerber;

import org.jmouse.cad.settings.CadSettings;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.exchange.ExportedFile;
import org.jmouse.cad.exchange.pcb.PcbArtwork;
import org.jmouse.cad.exchange.pcb.PcbFabrication;
import org.jmouse.cad.library.CadStandards;
import org.jmouse.gerber.GerberShape;
import org.jmouse.gerber.compose.GerberComposer;
import org.jmouse.strokefont.StrokeFont;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The files a pcb house makes a pcb from, named as KiCad names them: a Gerber X2 file for each copper layer, the
 * solder mask, the paste and the silkscreen of each side, the edge, the plated and the unplated holes as Excellon, the assembly tables by
 * a profile, the netlist the bare pcb is tested against, and a README. Millimetres, the pcb's origin, Gerber's Y up.
 */
public final class GerberSet {

    /**
     * What the set's own numbers are; the artwork's — the mask's expansion, the silkscreen's lines — are the pcb's,
     * shared with every format ({@link PcbArtwork.Settings}).
     *
     * @param edgeWidth the pen the pcb's edge is drawn with
     */
    public record Settings(double edgeWidth) {
    }

    /**
     * What a file of the set is, by how its name ends, for the README.
     *
     * @param ending how the file's name ends: -F_Cu.gbr
     * @param role   what the file is, as the set's README says it
     */
    public record FileRole(String ending, String role) {
    }

    /** A side of the pcb as its files name it. */
    private record Side(String id, String letter, String word) {
    }

    private static final List<Side> SIDES = List.of(new Side(PcbDrawing.TOP, "F", "Top"), new Side(PcbDrawing.BOTTOM, "B", "Bot"));

    private final Settings              settings;
    private final PcbArtwork.Settings artwork;
    private final StrokeFont            font;
    private final List<FileRole>        roles;

    private GerberSet(Settings settings, PcbArtwork.Settings artwork, List<FileRole> roles) {
        this.settings = settings;
        this.artwork = artwork;
        this.font = StrokeFont.newStroke();
        this.roles = List.copyOf(roles);
    }

    /** The set as this plugin ships it: its numbers and its README's words from the jar, the artwork's the pcb's. */
    public static GerberSet shipped() {
        return new GerberSet(CadSettings.read(GerberPlugin.FABRICATION),
                CadSettings.read(PcbArtwork.SECTION),
                List.of(CadSettings.read(GerberPlugin.FILES)));
    }

    /**
     * Every file of the set, in the order a reader expects them: copper from the top down, each side's mask and paste,
     * the edge, the drills, the assembly tables written by {@code profile}, the electrical test's netlist, the README.
     *
     * @param remarks what the README says beside the files: what the set does not carry yet
     */
    public List<ExportedFile> write(PcbFabrication fabrication, AssemblyProfile profile, List<String> remarks) {
        PcbDrawing pcb = fabrication.pcb();
        PcbArtwork drawn = PcbArtwork.of(fabrication, CadStandards.pcbRules().valuesOf(pcb.rules()), artwork, font);
        ArtworkGerber gerber = new ArtworkGerber(new PadShapes(drawn.outlines()));
        List<ExportedFile> files = new ArrayList<>();
        List<String> stack = pcb.copperLayerIds();
        for (int position = 0; position < stack.size(); position++) {
            String layer = stack.get(position);
            files.add(new ExportedFile(named(fabrication, copperName(layer, position)),
                    gerber.write("Copper,L" + (position + 1) + "," + sideWord(layer), true, drawn.copper(layer))));
        }
        for (Side side : SIDES) {
            if (stack.contains(side.id())) {
                // The mask is negative: what is drawn is left bare.
                files.add(new ExportedFile(named(fabrication, side.letter() + "_Mask.gbr"),
                        gerber.write("Soldermask," + side.word(), false, drawn.maskOpenings(side.id()))));
                files.add(new ExportedFile(named(fabrication, side.letter() + "_Paste.gbr"),
                        gerber.write("Paste," + side.word(), true, drawn.paste(side.id()))));
                files.add(new ExportedFile(named(fabrication, side.letter() + "_Silkscreen.gbr"),
                        gerber.write("Legend," + side.word(), true, drawn.silkscreen(side.id()))));
                files.add(new ExportedFile(named(fabrication, side.letter() + "_Assembly.gbr"),
                        gerber.write("AssemblyDrawing," + side.word(), true, drawn.assembly(side.id()))));
                // Glue only where something is glued: a set without any has no such file.
                if (!drawn.adhesive(side.id()).isEmpty()) {
                    files.add(new ExportedFile(named(fabrication, side.letter() + "_Adhesive.gbr"),
                            gerber.write("Glue," + side.word(), true, drawn.adhesive(side.id()))));
                }
            }
        }
        files.add(new ExportedFile(named(fabrication, "Edge_Cuts.gbr"), edge(drawn)));
        Drills.Programs drills = Drills.of(fabrication);
        files.add(new ExportedFile(named(fabrication, "PTH.drl"), drills.plated()));
        files.add(new ExportedFile(named(fabrication, "NPTH.drl"), drills.unplated()));
        files.addAll(AssemblyTables.of(fabrication, profile));
        files.add(new ExportedFile(named(fabrication, "netlist.ipc"),
                TestNetlist.of(fabrication, drawn.nets())));
        files.add(new ExportedFile("README.txt", readme(drawn, profile, files, remarks)));
        return files;
    }

    private static String named(PcbFabrication fabrication, String suffix) {
        return fabrication.name() + "-" + suffix;
    }

    /** KiCad's name for a copper layer's file: F_Cu, In1_Cu …, B_Cu. */
    private static String copperName(String layer, int position) {
        return switch (layer) {
            case PcbDrawing.TOP -> "F_Cu.gbr";
            case PcbDrawing.BOTTOM -> "B_Cu.gbr";
            default -> "In" + position + "_Cu.gbr";
        };
    }

    /** A copper layer's side as its file's function names it: Top, Bot, or Inr for one inside. */
    private static String sideWord(String layer) {
        return switch (layer) {
            case PcbDrawing.TOP -> "Top";
            case PcbDrawing.BOTTOM -> "Bot";
            default -> "Inr";
        };
    }

    /** The edge: every run of the outline drawn with a thin round pen. */
    private String edge(PcbArtwork drawn) {
        GerberComposer edge = PcbGerber.file("Profile,NP", true);
        int pen = edge.aperture(new GerberShape.Circle(settings.edgeWidth()), "Profile");
        for (List<double[]> run : drawn.runs(drawn.fabrication().pcb().outline())) {
            edge.line(pen, run.stream().map(point -> PcbGerber.out(point[0], point[1])).toList());
        }
        return edge.document().toSource();
    }

    /** What the set is, for whoever orders the pcb. */
    private String readme(PcbArtwork drawn, AssemblyProfile profile, List<ExportedFile> files, List<String> remarks) {
        PcbFabrication fabrication = drawn.fabrication();
        PcbDrawing pcb = fabrication.pcb();
        double[] extent = extentOf(drawn.runs(pcb.outline()));
        List<String> lines = new ArrayList<>();
        lines.add(fabrication.name() + " — fabrication files");
        lines.add("");
        lines.add(String.format(Locale.ROOT, "PCB: %.2f × %.2f mm, %d copper layer%s.", extent[0], extent[1],
                pcb.copperLayerIds().size(), pcb.copperLayerIds().size() == 1 ? "" : "s"));
        lines.add("Rules: " + pcb.rules().map(PcbDrawing.Rules::profile).orElse("standard") + ".");
        lines.add("Units: millimetres. Gerber X2, absolute coordinates; Excellon drills in decimal millimetres.");
        lines.add("Coordinates: the PCB's origin, Y upwards.");
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
