package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.Placement;
import org.jmouse.cad.pcb.drawing.Track;
import org.jmouse.cad.pcb.drawing.Via;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.CopperShape;
import org.jmouse.cad.pcb.geometry.CopperShapes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** A connection a routing laid, and the copper it laid for it: what may be lifted to make room. */
record LaidRoute(CopperNets.Unlaid line, List<Track> tracks, List<Via> vias, List<Placement> links) {

    /** A piece of a route's copper and the layer it stands on; a via stands on all of them. */
    record Copper(CopperShape shape, Optional<String> layer) {
    }

    /** What a layout gained over the one it was laid on: a laid connection only ever adds tracks, vias and links at the end. */
    static LaidRoute of(CopperNets.Unlaid line, PcbDrawing before, PcbDrawing after) {
        return new LaidRoute(line, after.tracks().subList(before.tracks().size(), after.tracks().size()),
                after.vias().subList(before.vias().size(), after.vias().size()),
                after.placements().subList(before.placements().size(), after.placements().size()));
    }

    /** The same things, by identity: a track the reader drew the same as a laid one is not the laid one. */
    static <T> Set<T> identities(List<T> things) {
        Set<T> set = Collections.newSetFromMap(new IdentityHashMap<>());
        set.addAll(things);
        return set;
    }

    /** A layout without this route's copper. */
    PcbDrawing liftedFrom(PcbDrawing pcb) {
        Set<Track> ownTracks = identities(tracks);
        Set<Via> ownVias = identities(vias);
        Set<Placement> ownLinks = identities(links);
        return pcb.withTracks(pcb.tracks().stream().filter(track -> !ownTracks.contains(track)).toList())
                .withVias(pcb.vias().stream().filter(via -> !ownVias.contains(via)).toList())
                .withPlacements(pcb.placements().stream().filter(placement -> !ownLinks.contains(placement)).toList());
    }

    /** Its tracks' points and its vias' middles. */
    List<Point> points() {
        List<Point> points = new ArrayList<>();
        tracks.forEach(track -> points.addAll(track.points()));
        vias.forEach(via -> points.add(new Point(via.x(), via.y())));
        return points;
    }

    /** Whether its copper comes within a margin of the box a connection spans: a route that may stand in its way. */
    boolean standsNear(CopperNets.Unlaid other, double margin) {
        double left = Math.min(other.from().x(), other.to().x()) - margin;
        double right = Math.max(other.from().x(), other.to().x()) + margin;
        double top = Math.min(other.from().y(), other.to().y()) - margin;
        double bottom = Math.max(other.from().y(), other.to().y()) + margin;
        return points().stream().anyMatch(point -> point.x() >= left && point.x() <= right && point.y() >= top && point.y() <= bottom);
    }

    /** How far it stands from a connection's middle: the nearest is lifted first. */
    double distanceTo(CopperNets.Unlaid other) {
        Point middle = other.from().halfwayTo(other.to());
        return points().stream().mapToDouble(point -> point.distanceTo(middle)).min().orElse(Double.POSITIVE_INFINITY);
    }

    /** Its copper, piece by piece. */
    List<Copper> copper(int curveSteps) {
        List<Copper> copper = new ArrayList<>();
        tracks.forEach(track -> copper.add(new Copper(CopperShapes.track(track, curveSteps), Optional.of(track.layer()))));
        vias.forEach(via -> copper.add(new Copper(CopperShapes.via(via), Optional.empty())));
        return copper;
    }
}
