package org.jmouse.cad.pcb.checks;

import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.Pair;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.NetLengths;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.jmouse.cad.document.FindingSeverity.WARNING;
import static org.jmouse.cad.pcb.geometry.CopperPieces.key;

/** A pair's tracks of another width, its two nets further apart in length than it allows, its gap nearer than the clearance. */
final class PairCheck implements PcbCheck {

    /** How far a track's width may differ from its pair's and still be it: what a width written to four places loses. */
    private static final double SAME_WIDTH = 1e-4;

    @Override
    public List<PcbFinding> findings(CheckContext context) {
        PcbDrawing pcb = context.pcb();
        if (pcb.pairs().isEmpty()) {
            return List.of();
        }
        double thickness = context.values().map(values -> values.getOrDefault(PcbVocabulary.RULE_PCB_THICKNESS, 0.0)).orElse(0.0);
        Map<String, NetLengths.Length> lengths = NetLengths.of(pcb, context.nets(), thickness);
        List<PcbFinding> findings = new ArrayList<>();
        for (Pair pair : pcb.pairs()) {
            List<String> otherWidth = new ArrayList<>();
            for (int index = 0; index < pcb.tracks().size(); index++) {
                Optional<String> net = context.nets().trackNets().get(index);
                if (net.filter(found -> found.equals(pair.positive()) || found.equals(pair.negative())).isPresent()
                    && Math.abs(pcb.tracks().get(index).width() - pair.width()) > SAME_WIDTH) {
                    otherWidth.add(key("track", index));
                }
            }
            if (!otherWidth.isEmpty()) {
                findings.add(new PcbFinding(WARNING, otherWidth.size() + " track" + (otherWidth.size() == 1 ? "" : "s") + " of the pair " + pair.name()
                        + " not " + FindingWords.written(pair.width()) + " mm wide", otherWidth));
            }
            double positive = Optional.ofNullable(lengths.get(pair.positive())).map(NetLengths.Length::total).orElse(0.0);
            double negative = Optional.ofNullable(lengths.get(pair.negative())).map(NetLengths.Length::total).orElse(0.0);
            if (pair.skew() > 0 && positive > 0 && negative > 0 && Math.abs(positive - negative) > pair.skew()) {
                List<String> keys = new ArrayList<>(context.copperOf(pair.positive()));
                keys.addAll(context.copperOf(pair.negative()));
                findings.add(new PcbFinding(WARNING, "The pair " + pair.name() + " is " + String.format(Locale.ROOT, "%.3f", Math.abs(positive - negative))
                        + " mm apart in length; it allows " + FindingWords.written(pair.skew()) + " mm", keys));
            }
            context.values().map(values -> FindingWords.rule(values, PcbVocabulary.RULE_CLEARANCE)).filter(clearance -> pair.gap() < clearance)
                    .ifPresent(clearance -> findings.add(new PcbFinding(WARNING, "The pair " + pair.name() + " is laid " + FindingWords.written(pair.gap())
                            + " mm apart, nearer than the PCB's clearance of " + FindingWords.written(clearance) + " mm", List.of())));
        }
        return findings;
    }
}
