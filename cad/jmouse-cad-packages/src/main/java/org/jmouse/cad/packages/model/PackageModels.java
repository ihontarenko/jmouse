package org.jmouse.cad.packages.model;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.document.TechnicalClass;
import org.jmouse.cad.mesh.ModelLinesParser;
import org.jmouse.cad.mesh.ModelMarkings;
import org.jmouse.cad.mesh.ModelModule;
import org.jmouse.cad.packages.footprint.FootprintDrawing;
import org.jmouse.cad.packages.footprint.FootprintGeneration.FormedFootprint;
import org.jmouse.cad.packages.footprint.StandardFootprint;
import org.jmouse.el.grammar.LineBuilder;
import org.jmouse.el.grammar.Nodes;
import org.jmouse.sheet.Sheet;
import org.jmouse.sheet.SheetEntry;
import org.jmouse.sheet.SheetValue;
import org.jmouse.svg.path.node.PathPolylines;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 🧊 The library's own 3D models, drawn from its footprints: each footprint of a package table that gives its body a
 * height becomes a model of boxes, cylinders and prisms ({@link ModelLinesParser}) — its body the widest closed outline of
 * its fabrication layer, moulded in two halves with its pin-one mark, its leads, terminations or pins where its pads are,
 * bent as its form bends them. What a footprint does not hold — a standoff, a moulding's thickness and draft, a lead's
 * section, the materials — is the table of package models ({@code package-models.sht}). The model stands in the
 * footprint's frame — millimetres, X to the right, Y up the drawing, Z out of the pcb — so the footprint names it with
 * no offset.
 */
public final class PackageModels {

    /** The fabrication layer, where a footprint draws its body's outline. */
    private static final String FABRICATION = "fabrication";

    /** How many straight steps a curve of an outline is drawn with, to measure the body by. */
    private static final int CURVE_STEPS = 8;

    /** The faces a body is printed on, as {@code print-face} names them: its top, or the front an upright body is read from. */
    private static final String TOP = "top";
    private static final String FRONT = "front";

    /** A marking as it lies, facing up Z. */
    private static final List<Double> UNTURNED = List.of(0.0, 0.0, 0.0);

    /** A face looking up +Z turned to look out along +Y, its up towards +Z: a quarter about X, then half round about Z. */
    private static final List<Double> FACING_FRONT = List.of(90.0, 0.0, 180.0);

    /** How many straight steps the round of a D-shaped body is drawn with. */
    private static final int ROUND_STEPS = 16;

    /** How thin a mark is drawn on a moulding's face: enough to stand clear of it, too little to be seen as a step. */
    private static final double MARK_DEPTH = 0.01;

    /** The pad a footprint numbers first: what its pin-one mark points at. */
    private static final String FIRST_PIN = "1";

    /** A model the tables give, the footprint it is drawn for, and the folder it is filed in — its footprint's. */
    public record StandardModel(String footprint, String slug, String folder, DocumentNode document) {
    }

    private final Map<String, SheetEntry> forms = new LinkedHashMap<>();

    public PackageModels(Sheet table) {
        table.entries().forEach(entry -> forms.put(entry.id(), entry));
    }

    /** A model for every footprint whose form the table draws and whose body has a height and an outline. */
    public List<StandardModel> generate(List<FormedFootprint> footprints) {
        List<StandardModel> models = new ArrayList<>();
        for (FormedFootprint formed : footprints) {
            SheetEntry form = forms.get(formed.form());
            if (form != null) {
                modelOf(formed.footprint(), new Form(form, formed.footprint().slug())).ifPresent(models::add);
            }
        }
        return models;
    }

    /** The slug a footprint's model is written under: its own, as a model. */
    public static String modelSlugOf(String footprintSlug) {
        String stem = footprintSlug.endsWith("-footprint") ? footprintSlug.substring(0, footprintSlug.length() - "-footprint".length())
                : footprintSlug;
        return stem + "-model";
    }

    private Optional<StandardModel> modelOf(StandardFootprint footprint, Form form) {
        FootprintDrawing drawing = footprint.drawing();
        Optional<Box> outline = bodyOutline(drawing);
        if (drawing.bodyHeight().isEmpty() || outline.isEmpty()) {
            return Optional.empty();
        }
        Part part = new Part(outline.get(), drawing.bodyHeight().getAsDouble(), drawing.pads(), form,
                form.word("materials").orElse(form.text("material")));
        List<LineBuilder> shapes = switch (form.id()) {
            case "chip" -> chip(part);
            case "gull-wing" -> gullWing(part);
            case "quad" -> quad(part);
            case "axial" -> axial(part);
            case "radial" -> radial(part);
            case "inline", "pinned" -> mounted(part);
            case "dual-inline" -> dualInline(part);
            case "header" -> header(part);
            default -> List.of();
        };
        if (shapes.isEmpty()) {
            return Optional.empty();
        }
        String slug = modelSlugOf(footprint.slug());
        DocumentNode document = new DocumentNode(ModelModule.MODEL, footprint.title(), slug, null, 1);
        String summary = footprint.title() + " — its body drawn by jmCAD from the footprint and its package table";
        // Its holder and licence its footprint's, and its source too: the body is drawn from that footprint's dimensions.
        MetaNode meta = MetaNode.filed(summary, TechnicalClass.MODEL.code(footprint.designation()), footprint.category(),
                footprint.rules().filing().tags(ModelModule.MODEL.word(), form.id()));
        footprint.rules().filing().provenance(footprint.source()).writeInto(meta);
        document.addExpression(meta);
        shapes.forEach(shape -> document.addExpression(shape.build()));
        return Optional.of(new StandardModel(footprint.slug(), slug, footprint.folder(), document));
    }

    /** What a model is drawn from: the body's outline and height, the pads, the form's row, and the body's material. */
    private record Part(Box body, double height, List<FootprintDrawing.Pad> pads, Form form, String material) {

        Optional<FootprintDrawing.Pad> firstPin() {
            return pads.stream().filter(pad -> FIRST_PIN.equals(pad.number())).findFirst();
        }
    }

    // ── Forms ─────────────────────────────────────────────────────────────────────────────────────────────────────

    /** A ceramic or moulded body between two terminations, at its two pads' ends. */
    private static List<LineBuilder> chip(Part part) {
        Box body = part.body();
        boolean acrossX = acrossX(part.pads());
        double length = acrossX ? body.width() : body.depth();
        double termination = length * part.form().number("termination-share");
        double proud = part.form().number("termination-proud");
        String terminal = part.form().text("terminal");
        List<LineBuilder> shapes = new ArrayList<>();
        shapes.add(box(part.material(), body.width(), body.depth(), part.height(), body.x(), body.y(), part.height() / 2));
        for (int side : new int[] {-1, 1}) {
            double offset = side * (length - termination) / 2;
            shapes.add(acrossX
                    ? box(terminal, termination, body.depth() + 2 * proud, part.height() + 2 * proud, body.x() + offset, body.y(),
                    part.height() / 2)
                    : box(terminal, body.width() + 2 * proud, termination, part.height() + 2 * proud, body.x(), body.y() + offset,
                    part.height() / 2));
        }
        return shapes;
    }

    /** A moulded body off the pcb, a gull-wing lead out of its side to every pad: a shoulder, a drop and a foot. */
    private static List<LineBuilder> gullWing(Part part) {
        Box body = part.body();
        Form form = part.form();
        double standoff = form.number("standoff");
        double thickness = form.number("lead-thickness");
        double shoulder = form.number("shoulder");
        String lead = form.text("lead");
        List<LineBuilder> shapes = new ArrayList<>(moulded(part, standoff, part.height() - standoff));
        for (FootprintDrawing.Pad pad : part.pads()) {
            double x = pad.x();
            double y = -pad.y();
            double width = Math.min(pad.width(), pad.height()) * form.number("lead-width-share");
            double dx = x - body.x();
            double dy = y - body.y();
            boolean sideways = Math.abs(dx) / body.width() >= Math.abs(dy) / body.depth();
            double direction = Math.signum(sideways ? dx : dy);
            double edge = sideways ? body.x() + direction * body.width() / 2 : body.y() + direction * body.depth() / 2;
            double reach = (sideways ? x : y) + direction * (sideways ? pad.width() : pad.height()) / 4;
            double rise = standoff + (part.height() - standoff) / 2;
            double footFrom = edge + direction * shoulder;
            double other = sideways ? y : x;
            shapes.add(along(lead, sideways, shoulder, width, thickness, edge + direction * shoulder / 2, other, rise));
            shapes.add(along(lead, sideways, thickness, width, rise, footFrom, other, rise / 2));
            shapes.add(along(lead, sideways, Math.abs(reach - footFrom), width, thickness, (reach + footFrom) / 2, other, thickness / 2));
        }
        return shapes;
    }

    /** A moulded body flat on the pcb, its lands the thin metal under its edges where the pads are. */
    private static List<LineBuilder> quad(Part part) {
        double land = part.form().number("land-thickness");
        List<LineBuilder> shapes = new ArrayList<>(moulded(part, land, part.height() - land));
        for (FootprintDrawing.Pad pad : part.pads()) {
            shapes.add(box(part.form().text("lead"), pad.width(), pad.height(), land, pad.x(), -pad.y(), land / 2));
        }
        return shapes;
    }

    /**
     * An axial body's profile, turned about its length, centred on its middle: its ends rounded off by the form's
     * {@code end-round} of its radius, and — where the package's name chooses a {@code waist} — a resistor's dog-bone, its
     * end caps {@code end-share} of its length at full radius, its middle narrowed to the waist.
     */
    private static List<double[]> axialProfile(Form form, double radius, double length) {
        double rounding = radius * form.number("end-round");
        double waist = radius * form.wordNumber("waist").orElse(1.0);
        double half = length / 2;
        List<double[]> profile = new ArrayList<>();
        profile.add(new double[] {0, -half});
        profile.add(new double[] {radius - rounding, -half});
        profile.add(new double[] {radius, -half + rounding});
        if (waist < radius) {
            double cap = length * form.number("end-share");
            double shoulder = radius - waist;
            profile.add(new double[] {radius, -half + cap});
            profile.add(new double[] {waist, -half + cap + shoulder});
            profile.add(new double[] {waist, half - cap - shoulder});
            profile.add(new double[] {radius, half - cap});
        }
        profile.add(new double[] {radius, half - rounding});
        profile.add(new double[] {radius - rounding, half});
        profile.add(new double[] {0, half});
        return profile;
    }

    /**
     * Where an axial body is marked, as its name chooses ({@code marking resistor = bands}): its waist — or its whole
     * length between its rounded ends, where it has none —, a hair over its surface.
     */
    private static Optional<LineBuilder> axialMarking(Form form, double radius, double length, double x, double y, double z,
                                                      double rotateX, double rotateY) {
        Optional<String> shows = form.word("marking");
        if (shows.isEmpty()) {
            return Optional.empty();
        }
        double waist = radius * form.wordNumber("waist").orElse(1.0);
        double span = waist < radius
                ? length * (1 - 2 * form.number("end-share")) - 2 * (radius - waist)
                : length - 2 * radius * form.number("end-round");
        return Optional.of(roundMarking(shows.get(), waist + MARK_DEPTH, span, 0, 360, x, y, z, rotateX, rotateY, Optional.empty(),
                ModelMarkings.Shows.BANDS.written().equals(shows.get()) ? Optional.empty() : Optional.of(form.text("print-ink"))));
    }

    /**
     * A part standing on end: its body over one pad, its leg straight down into that pad, and its other leg out of its top,
     * bent over and down into the other pad. A band, where it has one, is at the end nearest the pin it marks.
     */
    private static List<LineBuilder> standingAxial(Part part, List<FootprintDrawing.Pad> pads) {
        Box body = part.body();
        Form form = part.form();
        double below = form.number("below");
        String lead = form.text("lead");
        double height = part.height();
        double bend = height + form.number("standing-bend");
        double radius = body.width() / 2;
        List<LineBuilder> shapes = new ArrayList<>();
        shapes.add(turned(part.material(), axialProfile(form, radius, height), body.x(), body.y(), height / 2, 0, 0));
        axialMarking(form, radius, height, body.x(), body.y(), height / 2, 0, 0).ifPresent(shapes::add);
        FootprintDrawing.Pad under = pads.stream().min(Comparator.comparingDouble(pad -> Math.hypot(pad.x() - body.x(), -pad.y() - body.y()))).orElseThrow();
        FootprintDrawing.Pad beside = pads.stream().filter(pad -> pad != under).findFirst().orElseThrow();
        double leadRadius = under.drill() * form.number("lead-share");
        double underX = under.x();
        double underY = -under.y();
        double besideX = beside.x();
        double besideY = -beside.y();
        shapes.add(cylinder(lead, leadRadius, below, underX, underY, -below / 2, 0, 0, 0));
        shapes.add(cylinder(lead, leadRadius, bend - height, underX, underY, (height + bend) / 2, 0, 0, 0));
        shapes.add(cylinder(lead, leadRadius, bend + below, besideX, besideY, (bend - below) / 2, 0, 0, 0));
        boolean acrossX = acrossX(pads);
        double span = acrossX ? Math.abs(besideX - underX) : Math.abs(besideY - underY);
        shapes.add(acrossX
                ? cylinder(lead, leadRadius, span, (underX + besideX) / 2, underY, bend, 0, 90, 0)
                : cylinder(lead, leadRadius, span, underX, (underY + besideY) / 2, bend, 90, 0, 0));
        Optional<Double> band = form.wordNumber("band");
        Optional<FootprintDrawing.Pad> marked = part.firstPin();
        if (band.isPresent() && marked.isPresent()) {
            double share = band.get() * height;
            double along = marked.get() == under ? share * 1.5 : height - share * 1.5;
            shapes.add(cylinder(form.text("band-material"), radius + MARK_DEPTH, share, body.x(), body.y(), along, 0, 0, 0));
        }
        return shapes;
    }

    /**
     * An axial body: lying between its two holes, its height its diameter, turned with its ends rounded — a resistor's
     * caps and waist —, a band at its cathode where it is a diode; or standing over one hole when its outline is round,
     * its height its length. Each lead goes down through its hole.
     */
    private static List<LineBuilder> axial(Part part) {
        List<FootprintDrawing.Pad> pads = part.pads();
        if (pads.size() < 2) {
            return List.of();
        }
        Box body = part.body();
        Form form = part.form();
        double below = form.number("below");
        String lead = form.text("lead");
        List<LineBuilder> shapes = new ArrayList<>();
        boolean standing = Math.abs(body.width() - body.depth()) < 0.05 * Math.max(body.width(), body.depth());
        if (standing) {
            return standingAxial(part, pads);
        }
        boolean acrossX = acrossX(pads);
        double radius = part.height() / 2;
        double length = acrossX ? body.width() : body.depth();
        // Turned about Z, then laid along the pads: a quarter about Y lays it along X, a quarter about X along Y.
        double rotateX = acrossX ? 0 : 90;
        double rotateY = acrossX ? 90 : 0;
        shapes.add(turned(part.material(), axialProfile(form, radius, length), body.x(), body.y(), radius, rotateX, rotateY));
        axialMarking(form, radius, length, body.x(), body.y(), radius, rotateX, rotateY).ifPresent(shapes::add);
        // A diode's cathode is its band, at the pin it numbers first.
        Optional<Double> band = form.wordNumber("band");
        Optional<FootprintDrawing.Pad> cathode = part.firstPin();
        if (band.isPresent() && cathode.isPresent()) {
            double share = band.get() * length;
            double toward = acrossX ? Math.signum(cathode.get().x() - body.x()) : Math.signum(-cathode.get().y() - body.y());
            double offset = toward * (length / 2 - share * 1.5);
            String material = form.text("band-material");
            shapes.add(acrossX
                    ? cylinder(material, radius + MARK_DEPTH, share, body.x() + offset, body.y(), radius, 0, 90, 0)
                    : cylinder(material, radius + MARK_DEPTH, share, body.x(), body.y() + offset, radius, 90, 0, 0));
        }
        for (FootprintDrawing.Pad pad : pads) {
            double leadRadius = pad.drill() * form.number("lead-share");
            double x = pad.x();
            double y = -pad.y();
            double fromBody = acrossX ? Math.abs(x - body.x()) - length / 2 : Math.abs(y - body.y()) - length / 2;
            double out = acrossX ? Math.signum(x - body.x()) : Math.signum(y - body.y());
            if (fromBody > 0) {
                double middle = (acrossX ? body.x() : body.y()) + out * (length / 2 + fromBody / 2);
                shapes.add(acrossX
                        ? cylinder(lead, leadRadius, fromBody, middle, y, radius, 0, 90, 0)
                        : cylinder(lead, leadRadius, fromBody, x, middle, radius, 90, 0, 0));
            }
            shapes.add(cylinder(lead, leadRadius, radius + below, x, y, (radius - below) / 2, 0, 0, 0));
        }
        return shapes;
    }

    /**
     * A body standing on its leads, its outline its round. A can — a round body the form caps with metal — is turned as it
     * is made: its sleeve rounded at the bottom, beaded below its top and rolled over it, its metal top set in the roll
     * with the vent cross a larger can is scored with, its minus stripe facing its minus lead with its signs, and its value
     * printed round the other side. Any other round body is a cylinder, and one that is not round a box.
     */
    private static List<LineBuilder> radial(Part part) {
        Box body = part.body();
        Form form = part.form();
        double below = form.number("below");
        List<LineBuilder> shapes = new ArrayList<>();
        double radius = Math.min(body.width(), body.depth()) / 2;
        boolean round = Math.abs(body.width() - body.depth()) < 0.1 * Math.max(body.width(), body.depth());
        Optional<String> cap = form.word("cap");
        if (round && cap.isPresent()) {
            shapes.addAll(can(part, radius, cap.get()));
        } else {
            shapes.add(round ? cylinder(part.material(), radius, part.height(), body.x(), body.y(), part.height() / 2, 0, 0, 0)
                    : box(part.material(), body.width(), body.depth(), part.height(), body.x(), body.y(), part.height() / 2));
        }
        for (FootprintDrawing.Pad pad : part.pads()) {
            shapes.add(cylinder(form.text("lead"), pad.drill() * form.number("lead-share"), below, pad.x(), -pad.y(), -below / 2, 0, 0, 0));
        }
        return shapes;
    }

    /** An electrolytic can, as {@link #radial} tells it: every measure but its size the form's. */
    private static List<LineBuilder> can(Part part, double radius, String metal) {
        Box body = part.body();
        Form form = part.form();
        double height = part.height();
        double rounding = form.number("can-round");
        double lip = form.number("lip");
        double top = form.number("cap-height");
        double beadDepth = form.number("bead-depth");
        double beadWidth = form.number("bead-width");
        double bead = height - form.number("bead-below-top");
        // The sleeve, its profile from the middle of its bottom up its side, over its roll and in to the top it holds.
        List<double[]> sleeve = List.of(new double[] {0, 0}, new double[] {radius - rounding, 0}, new double[] {radius, rounding},
                new double[] {radius, bead - beadWidth / 2}, new double[] {radius - beadDepth, bead},
                new double[] {radius, bead + beadWidth / 2}, new double[] {radius, height - lip}, new double[] {radius - lip, height},
                new double[] {radius - lip, height - top}, new double[] {0, height - top});
        List<LineBuilder> shapes = new ArrayList<>();
        shapes.add(turned(part.material(), sleeve, body.x(), body.y(), 0, 0, 0));
        double inset = top / 2;
        shapes.add(cylinder(metal, radius - lip, top - inset, body.x(), body.y(), height - top + (top - inset) / 2, 0, 0, 0));
        if (radius >= form.number("vent-from")) {
            double reach = 2 * (radius - lip) * form.number("vent-share");
            double width = form.number("vent-width");
            double at = height - inset + MARK_DEPTH / 2;
            shapes.add(box(form.text("vent-material"), reach, width, MARK_DEPTH, body.x(), body.y(), at));
            shapes.add(box(form.text("vent-material"), width, reach, MARK_DEPTH, body.x(), body.y(), at));
        }
        // Its minus lead: the one it does not number first. The stripe faces it, the value the other way.
        Optional<FootprintDrawing.Pad> minus = part.pads().stream().filter(pad -> !FIRST_PIN.equals(pad.number())).findFirst();
        double facing = minus.map(pad -> Math.toDegrees(Math.atan2(-pad.y() - body.y(), pad.x() - body.x()))).orElse(0.0);
        double stripeArc = form.number("stripe-arc");
        double printArc = form.number("print-arc");
        double from = rounding;
        double to = bead - beadWidth / 2;
        double printed = radius + MARK_DEPTH;
        shapes.add(turned(form.text("stripe-material"), List.of(new double[] {printed, from}, new double[] {printed, to}),
                facing - stripeArc / 2, facing + stripeArc / 2, body.x(), body.y(), 0, 0, 0));
        double middle = (from + to) / 2;
        shapes.add(roundMarking(ModelMarkings.Shows.TEXT.written(), printed + MARK_DEPTH, to - from, facing - stripeArc / 2,
                facing + stripeArc / 2, body.x(), body.y(), middle, 0, 0, Optional.of(form.text("stripe-text")), Optional.of(form.text("stripe-ink"))));
        double opposite = facing + 180;
        shapes.add(roundMarking(ModelMarkings.Shows.VALUE.written(), printed, to - from, opposite - printArc / 2, opposite + printArc / 2,
                body.x(), body.y(), middle, 0, 0, Optional.empty(), Optional.of(form.text("print-ink"))));
        return shapes;
    }

    /**
     * A body off the pcb on straight leads — a transistor, a regulator, a trimmer, a relay, a transformer —: a box, or the
     * shape its form's `profile` names for the package: a TO-92's D, a round bridge's drum.
     */
    private static List<LineBuilder> mounted(Part part) {
        Box body = part.body();
        Form form = part.form();
        // A part on leads stands off the pcb; one with only surface lands lies on its terminations.
        boolean leaded = part.pads().stream().anyMatch(pad -> pad.drill() > 0);
        double standoff = leaded ? form.number("standoff") : 0;
        double below = form.number("below");
        List<LineBuilder> shapes = new ArrayList<>();
        double tall = part.height() - standoff;
        Optional<String> profile = form.word("profile");
        if (profile.filter("d"::equals).isPresent()) {
            shapes.add(prism(part.material(), dShape(body), tall, 0, 0, standoff));
        } else if (profile.filter("round"::equals).isPresent()) {
            shapes.add(cylinder(part.material(), Math.min(body.width(), body.depth()) / 2, tall, body.x(), body.y(), standoff + tall / 2, 0, 0, 0));
        } else {
            shapes.add(box(part.material(), body.width(), body.depth(), tall, body.x(), body.y(), standoff + tall / 2));
        }
        for (FootprintDrawing.Pad pad : part.pads()) {
            if (pad.drill() > 0) {
                double across = pad.drill() * form.number("lead-share") * 2;
                shapes.add(box(form.text("lead"), across, across, standoff + below, pad.x(), -pad.y(), (standoff - below) / 2));
            } else {
                // A surface termination: a tinned plate on its land, under the body where the two meet.
                double thickness = form.number("termination-thickness");
                shapes.add(box(form.text("lead"), pad.width(), pad.height(), thickness, pad.x(), -pad.y(), thickness / 2));
            }
        }
        shapes.addAll(topMarkings(part, standoff, part.height()));
        return shapes;
    }

    /**
     * What a body is printed with, as its name chooses — its value or its component's name, {@code print bridge = value},
     * {@code print name} —, on the face {@code print-face} says: the {@code top}, or the {@code front} an upright body is read
     * from (a TO-92's flat, a TO-220's face, towards +Y as its footprint stands it), up its middle. In the form's ink.
     */
    private static LineBuilder printed(Part part, String shows, double bottom, double top) {
        Box body = part.body();
        Form form = part.form();
        Optional<String> ink = Optional.of(form.text("print-ink"));
        double widthShare = form.number("print-width-share");
        double heightShare = form.number("print-height-share");
        if (FRONT.equals(form.word("print-face").orElse(TOP))) {
            double tall = top - bottom;
            return flatMarking(shows, body.width() * widthShare, tall * heightShare, body.x(), body.y() + body.depth() / 2 + MARK_DEPTH,
                    bottom + tall / 2, FACING_FRONT, Optional.empty(), ink);
        }
        return flatMarking(shows, body.width() * widthShare, Math.min(body.depth(), body.width()) * heightShare, body.x(), body.y(),
                top + MARK_DEPTH, UNTURNED, Optional.empty(), ink);
    }

    /**
     * What a body's top is marked with, as its name chooses: its print ({@link #printed}), and a sign over each pin it names,
     * in pad order ({@code pin-marks bridge = "+ ~ − ~"}) — a round bridge's {@code +}, {@code ~} and {@code −} stand over the
     * leads they name. A pin outside the body's outline has no top to be marked on, and is passed.
     */
    private static List<LineBuilder> topMarkings(Part part, double bottom, double top) {
        Box body = part.body();
        Form form = part.form();
        List<LineBuilder> markings = new ArrayList<>();
        double surface = top + MARK_DEPTH;
        form.word("print").ifPresent(shows -> markings.add(printed(part, shows, bottom, top)));
        Optional<String> signs = form.word("pin-marks");
        if (signs.isPresent()) {
            String[] written = signs.get().trim().split("\\s+");
            double size = form.number("pin-mark-size");
            List<FootprintDrawing.Pad> ordered = part.pads().stream()
                    .sorted(Comparator.comparing(FootprintDrawing.Pad::number, PackageModels::padOrder)).toList();
            for (int index = 0; index < Math.min(written.length, ordered.size()); index++) {
                FootprintDrawing.Pad pad = ordered.get(index);
                double x = pad.x();
                double y = -pad.y();
                boolean onTop = Math.abs(x - body.x()) <= body.width() / 2 - size / 2 && Math.abs(y - body.y()) <= body.depth() / 2 - size / 2;
                if (onTop) {
                    markings.add(flatMarking(ModelMarkings.Shows.TEXT.written(), size, size, x, y, surface, UNTURNED, Optional.of(written[index]),
                            Optional.of(form.text("print-ink"))));
                }
            }
        }
        return markings;
    }

    /** Pads in the order they are numbered: 2 before 10, and a lettered pad after the numbered ones. */
    private static int padOrder(String first, String second) {
        boolean firstNumbered = first.matches("\\d+");
        boolean secondNumbered = second.matches("\\d+");
        if (firstNumbered && secondNumbered) {
            return Integer.compare(Integer.parseInt(first), Integer.parseInt(second));
        }
        return firstNumbered != secondNumbered ? (firstNumbered ? -1 : 1) : first.compareTo(second);
    }

    /**
     * A DIP: a moulding off the pcb with its notch and pin-one dimple, each lead out of its side at the parting line, down
     * as a wide shoulder to the seating plane, then a narrow pin through its hole.
     */
    private static List<LineBuilder> dualInline(Part part) {
        Box body = part.body();
        Form form = part.form();
        double standoff = form.number("standoff");
        double thickness = form.wordNumber("body-thickness").orElse(part.height() - standoff);
        double leadThickness = form.number("lead-thickness");
        double shoulderWidth = form.number("shoulder-width");
        double pinWidth = form.number("lead-width");
        double below = form.number("below");
        String lead = form.text("lead");
        List<LineBuilder> shapes = new ArrayList<>(moulded(part, standoff, thickness));
        Optional<FootprintDrawing.Pad> first = part.firstPin();
        if (first.isPresent()) {
            // The notch: a round on the top at the end the first pin is at, within the moulding.
            double notch = form.number("notch-radius");
            double top = standoff + thickness + MARK_DEPTH / 2;
            boolean longAlongY = body.depth() >= body.width();
            shapes.add(longAlongY
                    ? cylinder(form.text("mark"), notch, MARK_DEPTH, body.x(),
                    body.y() + Math.signum(-first.get().y() - body.y()) * (body.depth() / 2 - form.number("draft") - notch), top, 0, 0, 0)
                    : cylinder(form.text("mark"), notch, MARK_DEPTH,
                    body.x() + Math.signum(first.get().x() - body.x()) * (body.width() / 2 - form.number("draft") - notch), body.y(), top, 0, 0, 0));
        }
        double parting = standoff + thickness / 2;
        for (FootprintDrawing.Pad pad : part.pads()) {
            if (pad.drill() <= 0) {
                continue;
            }
            double x = pad.x();
            double y = -pad.y();
            boolean sideways = Math.abs(x - body.x()) / body.width() >= Math.abs(y - body.y()) / body.depth();
            double direction = Math.signum(sideways ? x - body.x() : y - body.y());
            double edge = sideways ? body.x() + direction * body.width() / 2 : body.y() + direction * body.depth() / 2;
            double row = sideways ? x : y;
            double other = sideways ? y : x;
            double out = Math.abs(row - edge) + leadThickness / 2;
            if (out > 0) {
                shapes.add(along(lead, sideways, out, shoulderWidth, leadThickness, edge + direction * out / 2, other, parting));
            }
            // The shoulder down to the seating plane, then the narrow pin through the pcb.
            shapes.add(along(lead, sideways, leadThickness, shoulderWidth, parting - standoff, row, other, (parting + standoff) / 2));
            shapes.add(along(lead, sideways, leadThickness, pinWidth, standoff + below, row, other, (standoff - below) / 2));
        }
        return shapes;
    }

    /** A housing along the row, each pin through it, up to the header's height and down through its hole. */
    private static List<LineBuilder> header(Part part) {
        Box body = part.body();
        Form form = part.form();
        double housing = form.number("housing-height");
        double pin = form.number("pin-size");
        double below = form.number("below");
        List<LineBuilder> shapes = new ArrayList<>();
        shapes.add(box(part.material(), body.width(), body.depth(), housing, body.x(), body.y(), housing / 2));
        for (FootprintDrawing.Pad pad : part.pads()) {
            shapes.add(box(form.text("lead"), pin, pin, part.height() + below, pad.x(), -pad.y(), (part.height() - below) / 2));
        }
        return shapes;
    }

    /**
     * A moulded body from {@code bottom}, {@code thickness} tall: two halves at its parting line, the upper narrower by the
     * form's draft all round, a dimple over its first pin's corner, and its print on its top ({@link #topMarkings}).
     */
    private static List<LineBuilder> moulded(Part part, double bottom, double thickness) {
        Box body = part.body();
        Form form = part.form();
        double draft = form.number("draft");
        double half = thickness / 2;
        List<LineBuilder> shapes = new ArrayList<>();
        shapes.add(box(part.material(), body.width(), body.depth(), half, body.x(), body.y(), bottom + half / 2));
        shapes.add(box(part.material(), body.width() - 2 * draft, body.depth() - 2 * draft, half, body.x(), body.y(), bottom + half * 1.5));
        Optional<FootprintDrawing.Pad> first = part.firstPin();
        if (first.isPresent()) {
            double inset = form.number("mark-inset");
            double x = body.x() + Math.signum(first.get().x() - body.x()) * Math.max(0, body.width() / 2 - draft - inset);
            double y = body.y() + Math.signum(-first.get().y() - body.y()) * Math.max(0, body.depth() / 2 - draft - inset);
            shapes.add(cylinder(form.text("mark"), form.number("mark-radius"), MARK_DEPTH, x, y, bottom + thickness + MARK_DEPTH / 2, 0, 0, 0));
        }
        shapes.addAll(topMarkings(part, bottom, bottom + thickness));
        return shapes;
    }

    /** A TO-92's D: the round of the body's width, cut flat along the face its leads stand behind. */
    private static List<Double> dShape(Box body) {
        double radius = body.width() / 2;
        double flat = body.y() + body.depth() / 2;
        double centre = body.y() - body.depth() / 2 + radius;
        double reach = Math.max(-1, Math.min(1, (flat - centre) / radius));
        double from = Math.PI - Math.asin(reach);
        double to = 2 * Math.PI + Math.asin(reach);
        List<Double> outline = new ArrayList<>();
        for (int step = 0; step <= ROUND_STEPS; step++) {
            double angle = from + (to - from) * step / ROUND_STEPS;
            outline.add(round(body.x() + radius * Math.cos(angle)));
            outline.add(round(centre + radius * Math.sin(angle)));
        }
        return outline;
    }

    // ── Lines ─────────────────────────────────────────────────────────────────────────────────────────────────────

    /** A box {@code along} the lead's way, sideways along X or else along Y. */
    private static LineBuilder along(String material, boolean sideways, double along, double across, double tall, double at,
                                     double other, double z) {
        return sideways ? box(material, along, across, tall, at, other, z) : box(material, across, along, tall, other, at, z);
    }

    private static LineBuilder box(String material, double width, double depth, double height, double x, double y, double z) {
        return LineBuilder.line(ModelLinesParser.BOX).slot("material", Nodes.word(material))
                .clause("size", Nodes.numbers(List.of(round(width), round(depth), round(height))))
                .clause("at", Nodes.numbers(List.of(round(x), round(y), round(z))));
    }

    private static LineBuilder cylinder(String material, double radius, double length, double x, double y, double z,
                                        double rotateX, double rotateY, double rotateZ) {
        LineBuilder line = LineBuilder.line(ModelLinesParser.CYLINDER).slot("material", Nodes.word(material))
                .clause("radius", Nodes.number(round(radius))).clause("length", Nodes.number(round(length)))
                .clause("at", Nodes.numbers(List.of(round(x), round(y), round(z))));
        if (rotateX != 0 || rotateY != 0 || rotateZ != 0) {
            line.clause("rotate", Nodes.numbers(List.of(rotateX, rotateY, rotateZ)));
        }
        return line;
    }

    /** A body turned about Z from its {@code radius, z} profile, from one angle to another, then turned and set where it stands. */
    private static LineBuilder turned(String material, List<double[]> profile, double from, double to, double x, double y, double z,
                                      double rotateX, double rotateY) {
        LineBuilder line = LineBuilder.line(ModelLinesParser.REVOLVED).slot("material", Nodes.word(material))
                .clause("profile", Nodes.numbers(profile.stream().flatMap(pair -> java.util.stream.Stream.of(round(pair[0]), round(pair[1]))).toList()));
        if (from != 0 || to != 360) {
            line.clause("from", Nodes.number(round(from))).clause("to", Nodes.number(round(to)));
        }
        line.clause("at", Nodes.numbers(List.of(round(x), round(y), round(z))));
        if (rotateX != 0 || rotateY != 0) {
            line.clause("rotate", Nodes.numbers(List.of(rotateX, rotateY, 0.0)));
        }
        return line;
    }

    /** A whole turn of a body. */
    private static LineBuilder turned(String material, List<double[]> profile, double x, double y, double z, double rotateX, double rotateY) {
        return turned(material, profile, 0, 360, x, y, z, rotateX, rotateY);
    }

    /** Where a body is marked round its side ({@link ModelMarkings.Round}), and what with. */
    private static LineBuilder roundMarking(String shows, double radius, double length, double from, double to, double x, double y,
                                            double z, double rotateX, double rotateY, Optional<String> text, Optional<String> ink) {
        LineBuilder line = LineBuilder.line(ModelLinesParser.MARKING).slot("shows", Nodes.word(shows))
                .clause("on", Nodes.word(ModelMarkings.Round.WORD))
                .clause("radius", Nodes.number(round(radius))).clause("length", Nodes.number(round(length)));
        if (from != 0 || to != 360) {
            line.clause("from", Nodes.number(round(from))).clause("to", Nodes.number(round(to)));
        }
        line.clause("at", Nodes.numbers(List.of(round(x), round(y), round(z))));
        if (rotateX != 0 || rotateY != 0) {
            line.clause("rotate", Nodes.numbers(List.of(rotateX, rotateY, 0.0)));
        }
        text.ifPresent(words -> line.clause("text", Nodes.text(words)));
        ink.ifPresent(material -> line.clause("ink", Nodes.word(material)));
        return line;
    }

    /** Where a body is marked on a face looking up ({@link ModelMarkings.Flat}), and what with. */
    private static LineBuilder flatMarking(String shows, double width, double height, double x, double y, double z, List<Double> rotate,
                                           Optional<String> text,
                                           Optional<String> ink) {
        LineBuilder line = LineBuilder.line(ModelLinesParser.MARKING).slot("shows", Nodes.word(shows))
                .clause("on", Nodes.word(ModelMarkings.Flat.WORD))
                .clause("size", Nodes.numbers(List.of(round(width), round(height))))
                .clause("at", Nodes.numbers(List.of(round(x), round(y), round(z))));
        if (!rotate.equals(UNTURNED)) {
            line.clause("rotate", Nodes.numbers(rotate));
        }
        text.ifPresent(words -> line.clause("text", Nodes.text(words)));
        ink.ifPresent(material -> line.clause("ink", Nodes.word(material)));
        return line;
    }

    private static LineBuilder prism(String material, List<Double> outline, double height, double x, double y, double z) {
        return LineBuilder.line(ModelLinesParser.PRISM).slot("material", Nodes.word(material))
                .clause("outline", Nodes.numbers(outline)).clause("height", Nodes.number(round(height)))
                .clause("at", Nodes.numbers(List.of(round(x), round(y), round(z))));
    }

    /** A figure written to a thousandth of a millimetre: what a drawn body needs, and no arithmetic's dust. */
    private static double round(double value) {
        double rounded = Math.round(value * 1000) / 1000.0;
        return rounded == 0 ? 0 : rounded;
    }

    // ── The footprint's geometry ──────────────────────────────────────────────────────────────────────────────────

    /** A box of the model's frame: its centre and its size along X and Y. */
    private record Box(double x, double y, double width, double depth) {
    }

    /** Whether a part's pads lie along X rather than along Y: the way its body runs. */
    private static boolean acrossX(List<FootprintDrawing.Pad> pads) {
        double left = pads.stream().mapToDouble(FootprintDrawing.Pad::x).min().orElse(0);
        double right = pads.stream().mapToDouble(FootprintDrawing.Pad::x).max().orElse(0);
        double top = pads.stream().mapToDouble(FootprintDrawing.Pad::y).min().orElse(0);
        double bottom = pads.stream().mapToDouble(FootprintDrawing.Pad::y).max().orElse(0);
        return right - left >= bottom - top;
    }

    /** The box round the widest closed outline the footprint draws on its fabrication layer, in the model's frame. */
    private static Optional<Box> bodyOutline(FootprintDrawing drawing) {
        Box widest = null;
        for (FootprintDrawing.Outline outline : drawing.outlines()) {
            if (!FABRICATION.equals(outline.layer()) || outline.path() == null || outline.path().isEmpty()) {
                continue;
            }
            for (List<double[]> run : PathPolylines.of(outline.path(), CURVE_STEPS)) {
                if (run.size() < 3) {
                    continue;
                }
                double left = run.stream().mapToDouble(point -> point[0]).min().orElse(0);
                double right = run.stream().mapToDouble(point -> point[0]).max().orElse(0);
                double top = run.stream().mapToDouble(point -> point[1]).min().orElse(0);
                double bottom = run.stream().mapToDouble(point -> point[1]).max().orElse(0);
                Box box = new Box((left + right) / 2, -(top + bottom) / 2, right - left, bottom - top);
                if (widest == null || box.width() * box.depth() > widest.width() * widest.depth()) {
                    widest = box;
                }
            }
        }
        return Optional.ofNullable(widest);
    }

    /**
     * One row of the table of package models, read for one package: a form's numbers and materials, and those its name's
     * words choose ({@code materials diode = glass}).
     */
    private record Form(SheetEntry entry, String slug) {

        String id() {
            return entry.id();
        }

        String text(String field) {
            return entry.value(field).map(Form::written)
                    .orElseThrow(() -> new IllegalStateException("The package models of '" + entry.id() + "' write no " + field));
        }

        double number(String field) {
            return Double.parseDouble(text(field));
        }

        /**
         * A field written {@code word = value, …}: the value of the first word the package's name holds; a value written
         * alone, with no word, is every package's that names none ({@code print name}); none else.
         */
        Optional<String> word(String field) {
            Optional<SheetValue> written = entry.value(field);
            if (written.isEmpty()) {
                return Optional.empty();
            }
            List<SheetValue> items = written.get() instanceof SheetValue.Sequence sequence ? sequence.items() : List.of(written.get());
            Optional<String> every = Optional.empty();
            for (SheetValue item : items) {
                if (item instanceof SheetValue.Assignment(String word, SheetValue value)) {
                    if (slug.contains(word)) {
                        return Optional.of(written(value));
                    }
                } else if (every.isEmpty()) {
                    every = Optional.of(written(item));
                }
            }
            return every;
        }

        Optional<Double> wordNumber(String field) {
            return word(field).map(Double::parseDouble);
        }

        private static String written(SheetValue value) {
            return switch (value) {
                case SheetValue.Text text -> text.text();
                case SheetValue.Word word -> word.word();
                default -> value.written();
            };
        }
    }
}
