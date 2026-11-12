package org.jmouse.odb;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OdbJobTest {

    private static final OffsetDateTime WRITTEN = OffsetDateTime.of(2026, 10, 31, 12, 0, 0, 0, ZoneOffset.UTC);

    private static OdbFeatures.Point point(double x, double y) {
        return new OdbFeatures.Point(x, y);
    }

    @Test
    void featuresListEachSymbolOnceAndEveryFeatureInOrder() {
        OdbFeatures copper = new OdbFeatures()
                .line(point(0, 0), point(10, 0), new OdbSymbol.Round(0.25), true)
                .pad(point(10, 0), new OdbSymbol.Rectangle(1.6, 1.2), 0, true)
                .line(point(10, 0), point(10, 5.5), new OdbSymbol.Round(0.25), true)
                .arc(point(0, 0), point(2, 2), point(0, 2), new OdbSymbol.Round(0.25), true, true);
        assertEquals("""
                UNITS=MM
                #
                #Feature symbol names
                #
                $0 r250
                $1 rect1600x1200
                #
                #Layer features
                #
                L 0 0 10 0 0 P 0
                P 10 0 1 P 0 0
                L 10 0 10 5.5 0 P 0
                A 0 0 2 2 0 2 0 P 0 Y
                """, copper.text());
    }

    @Test
    void aSurfaceClosesEachContourAndCutsItsHoles() {
        OdbFeatures zone = new OdbFeatures().surface(
                new OdbFeatures.Contour(List.of(point(0, 0), point(4, 0), point(4, 4), point(0, 4))),
                List.of(new OdbFeatures.Contour(List.of(point(1, 1), point(2, 1), point(2, 2)))), true);
        assertTrue(zone.text().contains("""
                S P 0
                OB 0 0 I
                OS 4 0
                OS 4 4
                OS 0 4
                OS 0 0
                OE
                OB 1 1 H
                OS 2 1
                OS 2 2
                OS 1 1
                OE
                SE
                """));
    }

    @Test
    void theMatrixListsLayersTopToBottomAndTheDrillLayerItsSpan() {
        OdbJob job = new OdbJob("Amplifier Board", "pcb", new OdbJob.Origin("jmCAD", "jmCAD"), WRITTEN)
                .layer(OdbLayer.of("top", OdbLayer.Type.SIGNAL), new OdbFeatures())
                .layer(OdbLayer.of("bottom", OdbLayer.Type.SIGNAL), new OdbFeatures())
                .layer(OdbLayer.drill("drill", "top", "bottom"), new OdbFeatures());
        String matrix = job.files().get("matrix/matrix");
        assertTrue(matrix.indexOf("NAME=TOP") < matrix.indexOf("NAME=BOTTOM"));
        assertTrue(matrix.contains("TYPE=DRILL\n   NAME=DRILL\n   POLARITY=POSITIVE\n   START_NAME=TOP\n   END_NAME=BOTTOM\n"));
        assertTrue(job.files().get("misc/info").contains("JOB_NAME=amplifier-board\n"));
        assertTrue(job.files().get("misc/info").contains("SAVE_DATE=20261031.120000\n"));
    }

    @Test
    void theArchiveHoldsEveryFileUnderTheJobsFolder() throws IOException {
        OdbJob job = new OdbJob("amplifier", "pcb", new OdbJob.Origin("jmCAD", "jmCAD"), WRITTEN)
                .layer(OdbLayer.of("top", OdbLayer.Type.SIGNAL), new OdbFeatures().pad(point(1, 1), new OdbSymbol.Round(1), 0, true));
        Map<String, String> read = new LinkedHashMap<>();
        try (TarArchiveInputStream tar = new TarArchiveInputStream(new GzipCompressorInputStream(new ByteArrayInputStream(job.archive())))) {
            for (TarArchiveEntry entry = tar.getNextEntry(); entry != null; entry = tar.getNextEntry()) {
                read.put(entry.getName(), new String(tar.readAllBytes(), StandardCharsets.US_ASCII));
            }
        }
        assertEquals(List.of("amplifier/matrix/matrix", "amplifier/misc/info", "amplifier/fonts/standard",
                "amplifier/steps/pcb/stephdr", "amplifier/steps/pcb/profile", "amplifier/steps/pcb/layers/top/features"),
                List.copyOf(read.keySet()));
        assertTrue(read.get("amplifier/steps/pcb/layers/top/features").contains("P 1 1 0 P 0 0"));
    }

    @Test
    void aLayerIsNamedAsTheFormatAllows() {
        assertThrows(IllegalArgumentException.class, () -> OdbLayer.of("Top Copper", OdbLayer.Type.SIGNAL));
        assertThrows(IllegalArgumentException.class, () -> OdbLayer.of("drill", OdbLayer.Type.DRILL));
    }

    @Test
    void aNetLinksItsSubnetsToFeaturesByTheirNumber() {
        OdbFeatures top = new OdbFeatures();
        OdbFeatures.Point origin = new OdbFeatures.Point(0, 0);
        top.pad(origin, new OdbSymbol.Round(1), 0, true);
        int line = top.count();
        top.line(origin, new OdbFeatures.Point(1, 0), new OdbSymbol.Round(0.2), true);
        assertEquals(1, line);
        assertEquals(2, top.count());
        OdbEda eda = new OdbEda(List.of("top", "drill"));
        int vcc = eda.net("VCC");
        assertEquals(vcc, eda.net("VCC"));
        int ground = eda.net("my ground;1");
        int none = eda.net(null);
        assertEquals(2, none);
        int toeprint = eda.subnet(vcc, new OdbEda.Toeprint(OdbComponents.Side.BOTTOM, 3, 1));
        eda.feature(vcc, toeprint, OdbEda.FeatureType.COPPER, "top", 0);
        eda.feature(vcc, toeprint, OdbEda.FeatureType.HOLE, "drill", 7);
        eda.feature(ground, eda.subnet(ground, new OdbEda.Trace()), OdbEda.FeatureType.COPPER, "top", line);
        eda.subnet(ground, new OdbEda.Via());
        eda.subnet(none, new OdbEda.Plane());
        assertEquals(0, eda.packageOf(new OdbEda.Package("soic 8", 1.27, new OdbEda.Rectangle(-1, -2, 2, 4), List.of(
                new OdbEda.Pin("1", OdbEda.PinType.SURFACE, -0.5, 1, OdbEda.PinElectrical.ELECTRICAL, OdbEda.PinMount.SURFACE, new OdbEda.Circle(-0.5, 1, 0.25))))));
        assertEquals("""
                HDR jmouse
                UNITS=MM
                LYR top drill
                #
                # NET 0
                NET VCC
                SNT TOP B 3 1
                FID C 0 0
                FID H 1 7
                # NET 1
                NET my_ground_1
                SNT TRC
                FID C 0 1
                SNT VIA
                # NET 2
                NET $NONE$
                SNT PLN S E 0
                # PKG 0
                PKG soic_8 1.27 -1 -2 1 2
                RC -1 -2 2 4
                PIN 1 S -0.5 1 0 E S
                CR -0.5 1 0.25
                """, eda.text("jmouse"));
    }

    @Test
    void twoNamesThatReadAlikeOnceWrittenStayTwoAndAnUnknownLayerIsRefused() {
        OdbEda eda = new OdbEda(List.of("top"));
        eda.subnet(eda.net("a b"), new OdbEda.Via());
        eda.subnet(eda.net("a_b"), new OdbEda.Via());
        assertTrue(eda.text("jmouse").contains("NET a_b\n"));
        assertTrue(eda.text("jmouse").contains("NET a_b_2\n"));
        assertThrows(IllegalArgumentException.class, () -> eda.feature(0, 0, OdbEda.FeatureType.COPPER, "nowhere", 0));
    }

    @Test
    void aComponentIsWrittenWithItsToeprintsAndTheJobKeepsItOnItsLayer() {
        OdbComponents bottom = new OdbComponents(OdbComponents.Side.BOTTOM).add(new OdbComponents.Component(2, 10, -5, 90, "U 1", "tl072",
                List.of(new OdbComponents.Toeprint(0, 9, -5, 4, 1, "1"))));
        assertEquals("""
                UNITS=MM
                #
                # CMP 0
                CMP 2 10 -5 90 M U_1 tl072
                TOP 0 9 -5 90 M 4 1 1
                #
                """, bottom.text());
        OdbJob job = new OdbJob("board", "pcb", new OdbJob.Origin("jmouse", "person"), OffsetDateTime.parse("2026-11-02T10:00:00Z"));
        assertThrows(IllegalArgumentException.class, () -> job.components(bottom));
        job.layer(OdbLayer.of("comp_+_bot", OdbLayer.Type.COMPONENT), new OdbFeatures()).components(bottom).eda(new OdbEda(List.of("top")));
        assertTrue(job.files().containsKey("steps/pcb/layers/comp_+_bot/components"));
        assertTrue(job.files().get("steps/pcb/eda/data").startsWith("HDR jmouse\n"));
        assertTrue(job.files().get("matrix/matrix").contains("TYPE=COMPONENT"));
    }
}
