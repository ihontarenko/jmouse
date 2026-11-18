package org.jmouse.cad.pcb.routing;

import org.jmouse.settings.Label;
import org.jmouse.cad.settings.SettingsSection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * How the router lays copper: how it searches, how a track a reader carries is held, and how a whole pcb is routed. Data, a
 * section of the settings tree; what a reader chooses — the angle, jogs — comes with each request.
 *
 * @param routing     how a way is searched and shaped, whatever asks for it
 * @param carrying    how a track a reader drags is held to the pointer
 * @param banding     how the lines a moved part pulls or pushes are pulled tight, as rubber bands
 * @param pads        how a track leaves a pad
 * @param autorouting how a whole pcb's connections are laid
 * @param tuning      how a track is bent to a length
 */
public record RoutingSettings(@Label("Routing") Routing routing,
                              @Label("Carrying") Carrying carrying,
                              @Label("Banding") Banding banding,
                              @Label("Pads") Pads pads,
                              @Label("Autorouting") Autorouting autorouting,
                              @Label("Tuning") Tuning tuning) {

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/pcb/routing. */
    public static final SettingsSection<RoutingSettings> SECTION = new SettingsSection<>("pcb/routing", "Routing", RoutingSettings.class);

    /** An inner copper layer's id, by its number. */
    private static final Pattern INNER = Pattern.compile("^inner-(\\d+)$");

    /**
     * How a laid line is cleaned of what nobody means, and necked out of a fine pitch.
     *
     * @param shortestRun     a jog — a step shorter than this many track widths — goes from a line the router found
     * @param leastStep       a step shorter than this many track widths, which nobody can mean, goes from every line
     * @param worthShortening how much shorter a line has to come out to be laid again
     * @param fanOutReach     how far a neck out of a fine pitch may go
     * @param fanOutStep      how finely a neck's end is looked for
     * @param neckShares      how thin a neck may be, as shares of the track's width, the widest first; 0 is the rules' least
     * @param stopHalvings    how many halvings find where a track stops short of what it cannot pass
     * @param bucket          how wide a cell of a fence's index is: a few tracks' worth
     * @param corridorMargins how far past a way's two ends the room a corridor is searched through reaches, in millimetres, tried
     *                        nearest first: a way round little is found in a small room, one far round a part in a large one
     * @param relaidAtMost    how many tracks a layout may have laid again since the board was worked out whole and still be worked
     *                        out from it — the rest of its copper, nets and index kept, those tracks kept clear of on their own —:
     *                        past that, the board is worked out whole again, which then costs less
     * @param roundingHalvings how many halvings find the widest rounding a track's corners keep their gaps at
     */
    public record Routing(@Label("Shortest run") double shortestRun,
                          @Label("Least step") double leastStep,
                          @Label(value = "Worth shortening", unit = "mm") double worthShortening,
                          @Label(value = "Fan out reach", unit = "mm") double fanOutReach,
                          @Label(value = "Fan out step", unit = "mm") double fanOutStep,
                          @Label("Neck shares") List<Double> neckShares,
                          @Label("Stop halvings") int stopHalvings,
                          @Label(value = "Fence index cell", unit = "mm") double bucket,
                          @Label(value = "Corridor margins", unit = "mm") List<Double> corridorMargins,
                          @Label("Relaid at most") int relaidAtMost,
                          @Label("Rounding halvings") int roundingHalvings) {

        public Routing {
            neckShares = List.copyOf(neckShares);
            corridorMargins = List.copyOf(corridorMargins);
        }

        /**
         * How wide a cell of the grid a corridor's window is laid on: the nearest margin it is searched within. Windows a hand's steps
         * ask for near one another come out the very same window, and the copper united for one is the copper of the next.
         */
        public double corridorGrid() {
            double nearest = Double.POSITIVE_INFINITY;
            for (double margin : corridorMargins) {
                nearest = Math.min(nearest, margin);
            }
            return Double.isInfinite(nearest) ? 0 : nearest;
        }
    }

    /**
     * How a track follows a hand that drags it, a segment or a corner.
     *
     * @param holdSteps    how many halvings find how far a drag can go where it cannot go all the way
     * @param bandStep     how finely the point a track is pulled by moves towards the pointer
     * @param endStub      how far a corner carried stands out before the line goes on
     * @param widestDetour how much longer than straight a line going on from a carried corner may be
     * @param longestWalk  how much longer than the line asked for a walk round what is in the way may come out
     */
    public record Carrying(@Label("Hold steps") int holdSteps,
                           @Label(value = "Band step", unit = "mm") double bandStep,
                           @Label(value = "End stub", unit = "mm") double endStub,
                           @Label("Widest detour") double widestDetour,
                           @Label("Longest walk") double longestWalk) {
    }

    /**
     * How a track leaves a pad, laid by hand or laid again after a move.
     *
     * @param exit         {@code smart} — through the pad's best breakout, as KiCad's smart pads —, or {@code as-laid}
     * @param lookAhead    how many corners on from the pad a breakout may join the line at
     * @param slantedUpTo  how many times longer than wide a pad may be and still be left at a slant, through its corners: a square
     *                     one is; a thin one, only straight out of its sides
     */
    public record Pads(@Label("Exit") String exit,
                       @Label("Corners looked ahead") int lookAhead,
                       @Label("Left at a slant up to") double slantedUpTo) {

        /** How a track leaves a pad, as the word says. */
        public PadExit exitKind() {
            return PadExit.of(exit);
        }
    }

    /**
     * The lines a moved part pulls or pushes, pulled tight between the points that hold them as rubber bands are.
     *
     * @param reach      how far round where the moved parts' pads pass the drag works on the lines, in millimetres: past it a line
     *                   stands as it ran
     * @param stepShare  how far the part's copper moves between two pulls, as a share of the least gap and half width a band keeps:
     *                   under one, so nothing passes through a band between two pulls
     * @param mostSteps  the most pulls one drag is worked out in
     * @param margin     how far round a band the room it may be pulled through is looked at, in millimetres
     * @param heldNodes  which nodes ({@link CopperNodeKind}) hold a band where they stand besides pads, which always do:
     *                   {@code junction} — a third track meeting it —, {@code via}; a joint not named slides to where the lines meeting
     *                   at it are shortest
     * @param nodeRounds how many times the sliding joints are moved and their lines pulled again
     * @param pushPasses how many times, in one step, the bands another band was pushed into are pulled again round it
     * @param jointHalvings how many times a sliding joint's way toward where it is drawn is halved before it stays where it stands
     */
    public record Banding(@Label(value = "Reach", unit = "mm") double reach,
                         @Label("Step share") double stepShare,
                         @Label("Most steps") int mostSteps,
                         @Label(value = "Margin", unit = "mm") double margin,
                         @Label("Held joints") List<String> heldNodes,
                         @Label("Joint rounds") int nodeRounds,
                         @Label("Push passes") int pushPasses,
                         @Label("Joint halvings") int jointHalvings) {

        public Banding {
            heldNodes = List.copyOf(heldNodes);
        }

        /** Whether a kind of node holds a band where it stands: a pad always does. */
        public boolean holds(CopperNodeKind node) {
            return node == CopperNodeKind.PAD || heldNodes.stream().map(CopperNodeKind::of).anyMatch(node::equals);
        }
    }

    /** The way a layer's tracks run. */
    public enum Direction {
        HORIZONTAL, VERTICAL;

        public static Direction of(String word) {
            return valueOf(word.toUpperCase(Locale.ROOT));
        }
    }

    /**
     * Pads dropped onto their plane: how far out of the pad the via may stand, and how finely it is looked for, in millimetres.
     *
     * @param reach how far out of its pad a drop's via may stand
     * @param step  how finely the via's place is looked for
     */
    public record PlaneDrops(@Label(value = "Reach", unit = "mm") double reach,
                             @Label(value = "Step", unit = "mm") double step) {
    }

    /**
     * How connections the router refused are tried again by lifting what was laid.
     *
     * @param passes     how many times the refused are retried by lifting what was laid
     * @param candidates how many routes near a refused connection are lifted one at a time
     * @param margin     how far round a connection's box a route stands near it
     */
    public record RipUp(@Label("Passes") int passes,
                        @Label("Candidates") int candidates,
                        @Label(value = "Margin", unit = "mm") double margin) {
    }

    /**
     * A 0 Ω link over what is in the way: the footprint it stands on, the letters its designator starts with, and what it costs as a length of track.
     *
     * @param footprint  the footprint a 0 Ω link stands on, by slug
     * @param designator the letters a link is named with, before its number
     * @param cost       what a link costs a way, as the millimetres of track it is worth: dearer than a via, so it is laid only where nothing else
     *                   goes
     */
    public record Link(@Label("Footprint") String footprint,
                       @Label("Designator") String designator,
                       @Label(value = "Cost", unit = "mm") double cost) {
    }

    /**
     * How the autorouter lays a whole pcb's connections.
     *
     * @param angleStep        the directions an autorouted track may take where the reader does not lay at right angles
     * @param viaOffEnds       how far off the two ends of a connection a via stands at least
     * @param attempts         the most connections tried in a round
     * @param decimals         the places a laid point is written to
     * @param layerDirections  the way each outer layer's tracks run, so the layers cross rather than fight for the same channels
     * @param innerDirections  the ways inner layers' tracks run, one after another
     * @param againstDirection what a step across a layer's way costs, as a share of a step along it
     * @param budgetSeconds    how long a routing runs at most
     * @param planeDrops       how a pad is dropped onto its net's plane before anything is routed
     * @param ripUp            how a refused connection is retried by lifting what was laid
     * @param rounds           how many rounds a routing has, each giving the refused the room first
     * @param viaCost          what changing layer costs, as the millimetres of track it is worth
     * @param link             a 0 Ω link over what is in the way, where the reader allows one
     */
    public record Autorouting(@Label("Angle step") double angleStep,
                              @Label(value = "Via off ends", unit = "mm") double viaOffEnds,
                              @Label("Attempts") int attempts,
                              @Label("Decimals") int decimals,
                              @Label("Layer directions") Map<String, String> layerDirections,
                              @Label("Inner directions") List<String> innerDirections,
                              @Label("Against direction") double againstDirection,
                              @Label("Longest routing") double budgetSeconds,
                              @Label("Plane drops") PlaneDrops planeDrops,
                              @Label("Rip up") RipUp ripUp,
                              @Label("Rounds") int rounds,
                              @Label(value = "Via cost", unit = "mm") double viaCost,
                              @Label("Link") Link link) {

        public Autorouting {
            layerDirections = Map.copyOf(layerDirections);
            innerDirections = List.copyOf(innerDirections);
        }

        /** The way a layer's tracks run, as the settings say; empty for a layer they name no way for. */
        public Optional<Direction> directionOf(String layer) {
            Matcher inner = INNER.matcher(layer);
            if (inner.matches() && !innerDirections.isEmpty()) {
                return Optional.of(Direction.of(innerDirections.get((Integer.parseInt(inner.group(1)) - 1) % innerDirections.size())));
            }
            return Optional.ofNullable(layerDirections.get(layer)).map(Direction::of);
        }
    }

    /**
     * How a track is lengthened by meanders to match a length.
     *
     * @param amplitude  a meander's tallest bump
     * @param spacing    how far apart its sides stand, in track widths — never nearer than a track and its gap
     * @param tolerance  how near a length is brought: well inside any skew a pair allows
     * @param bumpsTried how many more bumps are tried, past the fewest the tallest bump allows, before a segment is too short
     */
    public record Tuning(@Label(value = "Amplitude", unit = "mm") double amplitude,
                         @Label("Spacing") double spacing,
                         @Label(value = "Tolerance", unit = "mm") double tolerance,
                         @Label("Bumps tried") int bumpsTried) {
    }
}
