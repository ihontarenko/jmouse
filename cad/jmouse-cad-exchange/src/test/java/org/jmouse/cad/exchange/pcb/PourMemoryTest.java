package org.jmouse.cad.exchange.pcb;

import org.jmouse.cad.library.CadLibrary;
import org.jmouse.cad.library.CadStandards;
import org.jmouse.cad.pcb.geometry.PourMemory;
import org.jmouse.strokefont.StrokeFont;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.algorithm.distance.DiscreteHausdorffDistance;
import org.locationtech.jts.geom.Geometry;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pours made from a memory of the last ones are the copper a pour from nothing makes, on the library's own pcbs: two pcbs whose
 * zone is the same — the second worked out only where its pieces differ from the first's —, and a pcb poured again unchanged.
 */
class PourMemoryTest {

    private static final PcbArtwork.Settings ARTWORK = settings();

    private static PcbArtwork.Settings settings() {
        try (InputStream stream = PcbArtwork.class.getResourceAsStream(PcbArtwork.SECTION.resource())) {
            return JsonMapper.builder().build().readValue(stream, PcbArtwork.Settings.class);
        } catch (IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    private static List<Optional<Geometry>> poured(String slug, PourMemory memory) {
        CadLibrary library = CadLibrary.library();
        PcbFabrication fabrication = PcbFabrication.of(library.find(slug).orElseThrow(), library::find);
        Map<String, Double> rules = CadStandards.shipped().pcbRules().valuesOf(fabrication.pcb().rules());
        return PcbArtwork.of(fabrication, rules, ARTWORK, StrokeFont.newStroke(), memory).pours();
    }

    private static void assertSameCopper(List<Optional<Geometry>> expected, List<Optional<Geometry>> actual, String what) {
        assertEquals(expected.size(), actual.size(), what);
        for (int index = 0; index < expected.size(); index++) {
            assertEquals(expected.get(index).isPresent(), actual.get(index).isPresent(), what + ": zone " + index + " poured or not");
            if (expected.get(index).isPresent()) {
                Geometry whole = expected.get(index).get();
                Geometry kept = actual.get(index).get();
                double apart = whole.symDifference(kept).getArea();
                assertTrue(apart < 1e-6, what + ": zone " + index + " differs from a whole pour by " + apart + " mm²");
                // A seam is a crack of no area: only the outlines tell it — every point of either edge on the other's.
                double farthest = DiscreteHausdorffDistance.distance(whole.getBoundary(), kept.getBoundary());
                assertTrue(farthest < 1e-3, what + ": zone " + index + "'s edge stands " + farthest + " mm off a whole pour's — a seam");
            }
        }
    }

    @Test
    void aSecondPcbOfTheSameZoneIsPouredAsFromNothing() {
        PourMemory memory = PourMemory.keeping(8);
        poured("bridge-with-capacitor-pcb", memory);
        assertSameCopper(poured("full-wave-rectifier-pcb", PourMemory.none()), poured("full-wave-rectifier-pcb", memory),
                "the full-wave rectifier after the bridge");
    }

    @Test
    void theOtherWayRoundToo() {
        PourMemory memory = PourMemory.keeping(8);
        poured("full-wave-rectifier-pcb", memory);
        assertSameCopper(poured("bridge-with-capacitor-pcb", PourMemory.none()), poured("bridge-with-capacitor-pcb", memory),
                "the bridge after the full-wave rectifier");
    }

    @Test
    void aPcbPouredAgainUnchangedIsTheSame() {
        PourMemory memory = PourMemory.keeping(8);
        List<Optional<Geometry>> first = poured("full-wave-rectifier-pcb", memory);
        assertSameCopper(first, poured("full-wave-rectifier-pcb", memory), "the full-wave rectifier poured again");
    }
}
