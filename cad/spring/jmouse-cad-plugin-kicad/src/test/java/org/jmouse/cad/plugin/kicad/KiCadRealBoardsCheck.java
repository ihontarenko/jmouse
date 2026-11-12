package org.jmouse.cad.plugin.kicad;

import org.jmouse.cad.settings.CadSettings;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.ExchangeFiles;
import org.jmouse.cad.exchange.ExportedFiles;
import org.jmouse.cad.exchange.ImportedDocuments;
import org.jmouse.cad.exchange.pcb.PcbArtwork;
import org.jmouse.cad.kicad.KiCadLibraries;
import org.jmouse.cad.kicad.KiCadLibraryMap;
import org.jmouse.cad.kicad.KiCadProjectExport;
import org.jmouse.cad.library.CadStandards;
import org.jmouse.cad.exchange.pcb.PcbFabrication;
import org.jmouse.cad.library.CadLibrary;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.PadOutline;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.footprint.KiCadFootprintLibraries;
import org.jmouse.kicad.footprint.KiCadPad;
import org.jmouse.kicad.footprint.KiCadPadType;
import org.jmouse.sexpression.SExpressionParser;
import org.jmouse.kicad.pcb.KiCadPcb;
import org.jmouse.kicad.pcb.KiCadPcbFootprint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Real boards through the whole import, on a machine that has them: {@code -Dkicad.real.projects=<folder>}. It holds the
 * import to what can be proved: every pad must land where KiCad puts it, and copper must end up on nets. Nothing here
 * runs without that folder.
 */
@EnabledIfSystemProperty(named = "kicad.real.projects", matches = ".+")
class KiCadRealBoardsCheck {

    /** A micrometre: what a pcb's coordinates are rounded to, and twice that for a pad reached through a rounded origin. */
    private static final double SAME_PLACE = 0.003;

    @Test
    void everyPadLandsWhereKiCadPutsIt() throws IOException {
        Path root = Path.of(System.getProperty("kicad.real.projects"));
        List<Path> boards;
        try (Stream<Path> walked = Files.walk(root)) {
            boards = walked.filter(path -> path.toString().endsWith(".kicad_pcb")).toList();
        }
        List<String> misplaced = new ArrayList<>();
        for (Path board : boards) {
            String text = Files.readString(board);
            KiCadPcb pcb = KiCadPcb.of(new SExpressionParser().parse(board.getFileName().toString(), text));
            if (pcb.footprints().isEmpty()) {
                System.out.println(root.relativize(board) + ": no footprints, skipped");
                continue;
            }
            ImportedDocuments imported = new KiCadDesignImport(() -> KiCadFootprintLibraries.NONE, () -> KiCadBoardImportTest.NO_MODELS)
                    .read(new ExchangeFiles(filesBeside(board), board.getFileName().toString(), null), CadLibrary.library());
            Map<String, DocumentNode> made = new HashMap<>();
            imported.library().forEach(document -> made.put(document.slug(), document));
            imported.designs().forEach(document -> made.put(document.slug(), document));
            CatalogueDocuments documents = slug -> Optional.ofNullable(made.get(slug)).or(() -> CadLibrary.library().find(slug));
            PcbFabrication fabrication = PcbFabrication.of(imported.root(), documents);
            int pads = misplacedPads(pcb, fabrication, misplaced);
            printDisagreements(pcb, fabrication);
            CopperNets nets = fabrication.copperNets(new PadOutline(16, 8), 16);
            long seated = fabrication.parts().size();
            System.out.println(root.relativize(board) + ": " + pcb.footprints().size() + " footprints, " + seated + " seated, "
                    + pads + " pads compared, " + nets.padNets().stream().filter(Optional::isEmpty).count() + "/" + nets.padNets().size()
                    + " pads on no net, " + nets.trackNets().stream().filter(Optional::isEmpty).count() + "/" + nets.trackNets().size()
                    + " tracks on no net, " + nets.viaNets().stream().filter(Optional::isEmpty).count() + "/" + nets.viaNets().size()
                    + " vias on no net, zones " + fabrication.pcb().zones().size() + ", keepouts " + fabrication.pcb().keepouts().size());
            imported.notes().stream().filter(note -> !note.contains("made from KiCad's symbol") && !note.contains("connect the design"))
                    .forEach(note -> System.out.println("    " + note.substring(0, Math.min(note.length(), 170))));
            fabrication.notes().forEach(note -> System.out.println("    fabrication: " + note));
        }
        misplaced.stream().limit(30).forEach(System.out::println);
        assertEquals(0, misplaced.size(), "pads away from where KiCad puts them");
    }

    /**
     * Every board written for KiCad reads back as the pcb it was: each part on its side, each pad where it stood on the net
     * it was on. The written projects are left in {@code -Dkicad.written=<folder>} when one is named, to be opened in KiCad.
     */
    @Test
    void aBoardWrittenForKiCadReadsBackAsItWas() throws IOException {
        Path root = Path.of(System.getProperty("kicad.real.projects"));
        Optional<Path> kept = Optional.ofNullable(System.getProperty("kicad.written")).map(Path::of);
        List<Path> boards;
        try (Stream<Path> walked = Files.walk(root)) {
            boards = walked.filter(path -> path.toString().endsWith(".kicad_pcb") && !path.toString().contains("-backups")).toList();
        }
        PcbArtwork.Settings artwork = CadSettings.read(PcbArtwork.SECTION);
        List<String> moved = new ArrayList<>();
        for (Path board : boards) {
            ImportedDocuments first = new KiCadDesignImport(() -> KiCadFootprintLibraries.NONE, () -> KiCadBoardImportTest.NO_MODELS)
                    .read(new ExchangeFiles(filesBeside(board), board.getFileName().toString(), null), CadLibrary.library());
            CatalogueDocuments firstDocuments = documentsOf(first);
            PcbFabrication before = PcbFabrication.of(first.root(), firstDocuments);
            if (before.parts().isEmpty()) {
                continue;
            }
            ExportedFiles written = KiCadProjectExport.of(first.root(), firstDocuments, KiCadLibraryMap.standard(), KiCadLibraries.standard(),
                    CadStandards.pcbRules().valuesOf(before.pcb().rules()), artwork);
            Map<String, String> files = new LinkedHashMap<>();
            written.files().forEach(file -> files.put(file.fileName(), file.text()));
            if (kept.isPresent()) {
                Path folder = kept.get().resolve(written.name());
                Files.createDirectories(folder);
                for (Map.Entry<String, String> file : files.entrySet()) {
                    Files.writeString(folder.resolve(file.getKey()), file.getValue());
                }
                Files.writeString(folder.resolve("notes.txt"), String.join(System.lineSeparator(), written.notes()));
            }
            String project = files.keySet().iterator().next();
            ImportedDocuments again = new KiCadDesignImport(() -> KiCadFootprintLibraries.NONE, () -> KiCadBoardImportTest.NO_MODELS)
                    .read(new ExchangeFiles(files, project, null), CadLibrary.library());
            PcbFabrication after = PcbFabrication.of(again.root(), documentsOf(again));
            int pads = 0;
            for (PcbFabrication.SeatedPart part : before.parts()) {
                Optional<PcbFabrication.SeatedPart> read = after.parts().stream().filter(candidate -> candidate.printed().equals(part.printed())
                        || candidate.designator().equals(part.designator())).findFirst();
                if (read.isEmpty() || part.drawing().isEmpty() || read.get().drawing().isEmpty()) {
                    moved.add(board.getFileName() + " " + part.designator() + " is not seated again");
                    continue;
                }
                for (FootprintDrawing.Pad pad : part.drawing().get().pads()) {
                    Optional<FootprintDrawing.Pad> other = read.get().drawing().get().pads().stream()
                            .filter(candidate -> candidate.number().equals(pad.number())).filter(candidate -> {
                                PcbDrawing.Point there = part.placement().placed(pad.x(), pad.y());
                                PcbDrawing.Point here = read.get().placement().placed(candidate.x(), candidate.y());
                                return Math.hypot(there.x() - here.x(), there.y() - here.y()) <= SAME_PLACE;
                            }).findFirst();
                    pads++;
                    if (other.isEmpty()) {
                        PcbDrawing.Point there = part.placement().placed(pad.x(), pad.y());
                        moved.add(board.getFileName() + " " + part.designator() + "." + pad.number() + " moved from " + there + " (part "
                                + part.placement() .x() + "," + part.placement().y() + " " + part.placement().rotation() + " " + part.placement().side()
                                + ", footprint " + part.footprint().orElse("?") + ") to part " + read.get().placement().x() + ","
                                + read.get().placement().y() + " " + read.get().placement().rotation() + " " + read.get().placement().side()
                                + " footprint " + read.get().footprint().orElse("?"));
                    }
                }
                if (!Objects.equals(part.padNets(), read.get().padNets())) {
                    moved.add(board.getFileName() + " " + part.designator() + " nets " + part.padNets() + " became " + read.get().padNets());
                }
            }
            System.out.println(root.relativize(board) + ": written and read again, " + before.parts().size() + " parts, " + pads
                    + " pads; tracks " + before.pcb().tracks().size() + " -> " + after.pcb().tracks().size() + ", arcs "
                    + before.pcb().arcs().size() + " -> " + after.pcb().arcs().size() + ", vias " + before.pcb().vias().size() + " -> "
                    + after.pcb().vias().size() + ", zones " + before.pcb().zones().size() + " -> " + after.pcb().zones().size());
            written.notes().forEach(note -> System.out.println("    " + note.substring(0, Math.min(note.length(), 200))));
        }
        // Each board's first few, so one board's many do not hide another's.
        Map<String, List<String>> byBoard = new LinkedHashMap<>();
        moved.forEach(line -> byBoard.computeIfAbsent(line.substring(0, line.indexOf(".kicad_pcb")), ignored -> new ArrayList<>()).add(line));
        byBoard.forEach((board, lines) -> {
            System.out.println(board + ": " + lines.size() + " came back otherwise");
            lines.stream().limit(5).forEach(line -> System.out.println("    " + line));
        });
        assertEquals(0, moved.size(), "what came back other than it was written");
    }

    private static CatalogueDocuments documentsOf(ImportedDocuments imported) {
        Map<String, DocumentNode> made = new HashMap<>();
        imported.library().forEach(document -> made.put(document.slug(), document));
        imported.designs().forEach(document -> made.put(document.slug(), document));
        made.put(imported.root().slug(), imported.root());
        return slug -> Optional.ofNullable(made.get(slug)).or(() -> CadLibrary.library().find(slug));
    }

    /** Each board net's pads, by the design's net they are on: a net split or joined differently is plain from it. */
    private static void printDisagreements(KiCadPcb pcb, PcbFabrication fabrication) {
        Map<String, PcbFabrication.SeatedPart> byPrinted = new LinkedHashMap<>();
        fabrication.parts().forEach(part -> byPrinted.put(part.printed(), part));
        Map<String, Map<String, List<String>>> byBoardNet = new java.util.TreeMap<>();
        List<String> unwired = new ArrayList<>();
        for (KiCadPcbFootprint footprint : pcb.footprints()) {
            PcbFabrication.SeatedPart part = byPrinted.get(footprint.reference());
            if (part == null) {
                continue;
            }
            footprint.padNets().forEach((pad, net) -> {
                String ours = part.padNets().get(pad);
                if (ours == null) {
                    unwired.add(footprint.reference() + "." + pad + "(" + part.component() + ") board " + net);
                    return;
                }
                byBoardNet.computeIfAbsent(net, ignored -> new java.util.TreeMap<>()).computeIfAbsent(ours, ignored -> new ArrayList<>())
                        .add(footprint.reference() + "." + pad + "(" + part.component() + ")");
            });
        }
        byBoardNet.forEach((net, designed) -> {
            if (designed.size() > 1) {
                System.out.println("    SPLIT " + net + " -> " + designed.toString().substring(0, Math.min(designed.toString().length(), 900)));
            }
        });
        if (!unwired.isEmpty()) {
            System.out.println("    UNWIRED " + unwired.size() + ": " + unwired.subList(0, Math.min(unwired.size(), 10)));
        }
    }

    private static int misplacedPads(KiCadPcb pcb, PcbFabrication fabrication, List<String> misplaced) {
        Map<String, PcbFabrication.SeatedPart> byPrinted = new LinkedHashMap<>();
        fabrication.parts().forEach(part -> byPrinted.put(part.printed(), part));
        int compared = 0;
        for (KiCadPcbFootprint footprint : pcb.footprints()) {
            // A logo or a frame: no reference to find it by, and no pad to be wrong about.
            if (footprint.reference().isBlank()) {
                continue;
            }
            PcbFabrication.SeatedPart part = byPrinted.get(footprint.reference());
            if (part == null || part.drawing().isEmpty()) {
                misplaced.add(footprint.reference() + " is not seated on " + pcb.name());
                continue;
            }
            PcbDrawing.Placement placement = part.placement();
            double left = footprint.at().x() - placement.x();
            double top = footprint.at().y() - placement.y();
            Map<String, FootprintDrawing.Pad> ours = new LinkedHashMap<>();
            part.drawing().get().pads().forEach(pad -> ours.putIfAbsent(key(pad.number(), pad.x(), pad.y()), pad));
            for (KiCadPad pad : footprint.footprint().pads()) {
                // A window in the paste is no pad of the part, and is not on its footprint.
                if (pad.layers().stream().noneMatch(layer -> layer.endsWith(".Cu")) && pad.type() != KiCadPadType.UNPLATED_HOLE) {
                    continue;
                }
                FootprintDrawing.Pad drawn = ours.get(key(pad.number(), pad.at().x(), pad.at().y()));
                if (drawn == null) {
                    misplaced.add(footprint.reference() + " pad " + pad.number() + " is not on its footprint on " + pcb.name());
                    continue;
                }
                KiCadPoint theirs = footprint.onBoard(pad.at());
                PcbDrawing.Point placed = placement.placed(drawn.x(), drawn.y());
                compared++;
                if (Math.abs(theirs.x() - left - placed.x()) > SAME_PLACE || Math.abs(theirs.y() - top - placed.y()) > SAME_PLACE) {
                    misplaced.add(footprint.reference() + " pad " + pad.number() + " on " + pcb.name() + " " + footprint.side() + " "
                            + footprint.angle() + "°: KiCad " + (theirs.x() - left) + "," + (theirs.y() - top) + ", ours " + placed.x() + "," + placed.y());
                }
            }
        }
        return compared;
    }

    private static String key(String number, double x, double y) {
        return number + "@" + Math.round(x * 100) + "," + Math.round(y * 100);
    }

    /** What the explorer sends with a board: everything of KiCad's beside it, under its folder. */
    private static Map<String, String> filesBeside(Path board) throws IOException {
        Path folder = board.getParent();
        Map<String, String> files = new LinkedHashMap<>();
        try (Stream<Path> walked = Files.walk(folder)) {
            for (Path path : walked.filter(Files::isRegularFile).toList()) {
                String name = path.toString();
                if (name.endsWith(".kicad_sch") || name.endsWith(".kicad_pcb") || name.endsWith(".kicad_mod") || name.endsWith(".kicad_pro")) {
                    if (!name.contains("-backups")) {
                        files.put(folder.relativize(path).toString().replace('\\', '/'), Files.readString(path));
                    }
                }
            }
        }
        return files;
    }
}
