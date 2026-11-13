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
 * section of the settings tree; what a reader chooses — the angle, walk or shove, jogs — comes with each request.
 *
 * @param routing     how a way is searched and shaped, whatever asks for it
 * @param carrying    how a track a reader drags is held to the pointer
 * @param autorouting how a whole pcb's connections are laid
 * @param tuning      how a track is bent to a length
 */
public record RoutingSettings(@Label("Routing") Routing routing,
                              @Label("Carrying") Carrying carrying,
                              @Label("Autorouting") Autorouting autorouting,
                              @Label("Tuning") Tuning tuning) {

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/pcb/routing. */
    public static final SettingsSection<RoutingSettings> SECTION = new SettingsSection<>("pcb/routing", "Routing", RoutingSettings.class);

    /** An inner copper layer's id, by its number. */
    private static final Pattern INNER = Pattern.compile("^inner-(\\d+)$");

    /**
     * One search over a grid: its step as a share of a track's width and gap, and how far round the two ends it looks, in steps.
     *
     * @param share  the grid's step, as a share of half a track's width and gap together
     * @param margin how far past the leg's two ends the grid reaches, in its own steps
     */
    public record Search(@Label("Grid step share") double share,
                         @Label("Margin") double margin) {
    }

    /**
     * @param searches        the searches tried one after another, quick and coarse first
     * @param turnCost        what a corner costs a way, in steps
     * @param searchLimit     the most grid points a search looks at, laying copper that has the time
     * @param shortestRun     a jog — a step shorter than this many track widths — goes from a line the router found
     * @param leastStep       a step shorter than this many track widths, which nobody can mean, goes from every line
     * @param worthShortening how much shorter a line has to come out to be laid again
     * @param shoveSampling   how often a track shoved aside is looked at along its length
     * @param fanOutReach     how far a neck out of a fine pitch may go
     * @param fanOutStep      how finely a neck's end is looked for
     * @param neckShares      how thin a neck may be, as shares of the track's width, the widest first; 0 is the rules' least
     * @param stopHalvings    how many halvings find where a track stops short of what it cannot pass
     * @param bucket          how wide a cell of a fence's index is: a few tracks' worth
     * @param roomLooked      how far round a point its room is looked for: a few steps of the finest search
     */
    public record Routing(@Label("Searches") List<Search> searches,
                          @Label("Turn cost") double turnCost,
                          @Label("Search limit") int searchLimit,
                          @Label("Shortest run") double shortestRun,
                          @Label("Least step") double leastStep,
                          @Label(value = "Worth shortening", unit = "mm") double worthShortening,
                          @Label(value = "Shove sampling", unit = "mm") double shoveSampling,
                          @Label(value = "Fan out reach", unit = "mm") double fanOutReach,
                          @Label(value = "Fan out step", unit = "mm") double fanOutStep,
                          @Label("Neck shares") List<Double> neckShares,
                          @Label("Stop halvings") int stopHalvings,
                          @Label(value = "Fence index cell", unit = "mm") double bucket,
                          @Label(value = "Room looked round a point", unit = "mm") double roomLooked) {

        public Routing {
            searches = List.copyOf(searches);
            neckShares = List.copyOf(neckShares);
        }
    }

    /**
     * @param searchLimit  the most grid points a search looks at while a reader drags: it has a frame to answer in
     * @param holdSteps    how many halvings find how far a drag can go where it cannot go all the way
     * @param bandStep     how finely the point a track is pulled by moves towards the pointer
     * @param frameBudget  how long the tracks a moved part pulls are searched for, a frame at a time, in milliseconds
     * @param endStub      how far a corner carried stands out before the line goes on
     * @param widestDetour how much longer than straight a line going on from a carried corner may be
     * @param longestWalk  how much longer than the line asked for a walk round what is in the way may come out
     */
    public record Carrying(@Label("Search limit") int searchLimit,
                           @Label("Hold steps") int holdSteps,
                           @Label(value = "Band step", unit = "mm") double bandStep,
                           @Label("Frame budget") long frameBudget,
                           @Label(value = "End stub", unit = "mm") double endStub,
                           @Label("Widest detour") double widestDetour,
                           @Label("Longest walk") double longestWalk) {
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
     * @param passes     how many times the refused are retried by lifting what was laid
     * @param candidates how many routes near a refused connection are lifted one at a time
     * @param margin     how far round a connection's box a route stands near it
     */
    public record RipUp(@Label("Passes") int passes,
                        @Label("Candidates") int candidates,
                        @Label(value = "Margin", unit = "mm") double margin) {
    }

    /**
     * A 0 Ω link over what is in the way: the footprint it stands on, the letters its designator starts with, and what it costs in steps.
     *
     * @param footprint  the footprint a 0 Ω link stands on, by slug
     * @param designator the letters a link is named with, before its number
     * @param cost       what a link costs a way, in grid steps: dearer than a via, so it is laid only where nothing else
     *                   goes
     */
    public record Link(@Label("Footprint") String footprint,
                       @Label("Designator") String designator,
                       @Label("Cost") double cost) {
    }

    /**
     * @param angleStep        the directions an autorouted track may take where the reader does not lay at right angles
     * @param viaOffEnds       how far off the two ends of a connection a via stands at least
     * @param attempts         the most connections tried in a round
     * @param decimals         the places a laid point is written to
     * @param layerDirections  the way each outer layer's tracks run, so the layers cross rather than fight for the same
     *                         channels
     * @param innerDirections  the ways inner layers' tracks run, one after another
     * @param againstDirection what a step across a layer's way costs, as a share of a step along it
     * @param budgetSeconds    how long a routing runs at most
     * @param planeDrops       how a pad is dropped onto its net's plane before anything is routed
     * @param ripUp            how a refused connection is retried by lifting what was laid
     * @param rounds           how many rounds a routing has, each giving the refused the room first
     * @param viaCost          what changing layer costs, in steps
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
                              @Label("Via cost") double viaCost,
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
