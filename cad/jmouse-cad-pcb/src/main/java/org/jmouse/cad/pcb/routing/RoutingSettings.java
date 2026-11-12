package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.settings.SettingsOwner;
import org.jmouse.cad.settings.SettingsSection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * How the router lays copper: how it searches, how a track a reader carries is held, and how a whole pcb is routed. Data,
 * beside this class ({@link #SECTION}); what a reader chooses — the angle, walk or shove, jogs — comes with each request.
 *
 * @param routing     how a way is searched and shaped, whatever asks for it
 * @param carrying    how a track a reader drags is held to the pointer
 * @param autorouting how a whole pcb's connections are laid
 * @param tuning      how a track is bent to a length
 */
public record RoutingSettings(Routing routing, Carrying carrying, Autorouting autorouting, Tuning tuning) {

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/pcb/routing. */
    public static final SettingsSection<RoutingSettings> SECTION = new SettingsSection<>("pcb/routing", "Routing", SettingsOwner.ENGINE_SETTINGS, RoutingSettings.class);

    /** An inner copper layer's id, by its number. */
    private static final Pattern INNER = Pattern.compile("^inner-(\\d+)$");

    /**
     * One search over a grid: its step as a share of a track's width and gap, and how far round the two ends it looks, in steps.
     *
     * @param share  the grid's step, as a share of half a track's width and gap together
     * @param margin how far past the leg's two ends the grid reaches, in its own steps
     */
    public record Search(double share, double margin) {
    }

    /**
     * @param searches        the searches tried one after another, quick and coarse first
     * @param turnCost        what a corner costs a way, in steps
     * @param searchLimit     the most grid points a search looks at, laying copper that has the time
     * @param shortestRun     a jog — a step shorter than this many track widths — goes from a line the router found
     * @param leastStep       a step shorter than this many track widths, which nobody can mean, goes from every line
     * @param worthShortening how much shorter, in millimetres, a line has to come out to be laid again
     * @param shoveSampling   how often a track shoved aside is looked at along its length, in millimetres
     * @param fanOutReach     how far a neck out of a fine pitch may go, in millimetres
     * @param fanOutStep      how finely a neck's end is looked for, in millimetres
     * @param neckShares      how thin a neck may be, as shares of the track's width, the widest first; 0 is the rules' least
     * @param stopHalvings    how many halvings find where a track stops short of what it cannot pass
     * @param bucket          how wide a cell of a fence's index is, in millimetres: a few tracks' worth
     * @param roomLooked      how far round a point its room is looked for, in millimetres: a few steps of the finest search
     */
    public record Routing(List<Search> searches, double turnCost, int searchLimit, double shortestRun, double leastStep, double worthShortening,
                          double shoveSampling, double fanOutReach, double fanOutStep, List<Double> neckShares, int stopHalvings, double bucket,
                          double roomLooked) {

        public Routing {
            searches = List.copyOf(searches);
            neckShares = List.copyOf(neckShares);
        }
    }

    /**
     * @param searchLimit  the most grid points a search looks at while a reader drags: it has a frame to answer in
     * @param holdSteps    how many halvings find how far a drag can go where it cannot go all the way
     * @param bandStep     how finely the point a track is pulled by moves towards the pointer, in millimetres
     * @param frameBudget  how long the tracks a moved part pulls are searched for, a frame at a time, in milliseconds
     * @param endStub      how far a corner carried stands out before the line goes on, in millimetres
     * @param widestDetour how much longer than straight a line going on from a carried corner may be
     * @param longestWalk  how much longer than the line asked for a walk round what is in the way may come out
     */
    public record Carrying(int searchLimit, int holdSteps, double bandStep, long frameBudget, double endStub, double widestDetour,
                           double longestWalk) {
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
     * @param reach how far out of its pad a drop's via may stand, in millimetres
     * @param step  how finely the via's place is looked for, in millimetres
     */
    public record PlaneDrops(double reach, double step) {
    }

    /**
     * @param passes     how many times the refused are retried by lifting what was laid
     * @param candidates how many routes near a refused connection are lifted one at a time
     * @param margin     how far round a connection's box a route stands near it, in millimetres
     */
    public record RipUp(int passes, int candidates, double margin) {
    }

    /**
     * A 0 Ω link over what is in the way: the footprint it stands on, the letters its designator starts with, and what it costs in steps.
     *
     * @param footprint  the footprint a 0 Ω link stands on, by slug
     * @param designator the letters a link is named with, before its number
     * @param cost       what a link costs a way, in grid steps: dearer than a via, so it is laid only where nothing else
     *                   goes
     */
    public record Link(String footprint, String designator, double cost) {
    }

    /**
     * @param angleStep        the directions an autorouted track may take where the reader does not lay at right angles
     * @param viaOffEnds       how far off the two ends of a connection a via stands at least, in millimetres
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
    public record Autorouting(double angleStep, double viaOffEnds, int attempts, int decimals, Map<String, String> layerDirections,
                              List<String> innerDirections, double againstDirection, double budgetSeconds, PlaneDrops planeDrops, RipUp ripUp,
                              int rounds, double viaCost, Link link) {

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
     * @param amplitude  a meander's tallest bump, in millimetres
     * @param spacing    how far apart its sides stand, in track widths — never nearer than a track and its gap
     * @param tolerance  how near a length is brought, in millimetres: well inside any skew a pair allows
     * @param bumpsTried how many more bumps are tried, past the fewest the tallest bump allows, before a segment is too short
     */
    public record Tuning(double amplitude, double spacing, double tolerance, int bumpsTried) {
    }
}
