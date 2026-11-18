package org.jmouse.cad.pcb.checks;

import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.NetClass;
import org.jmouse.cad.pcb.drawing.NetRules;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.CopperPieces;
import org.jmouse.cad.pcb.geometry.CopperShape;
import org.jmouse.geometry.index.BoxIndex;
import org.jmouse.geometry.index.BoxTree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.jmouse.cad.document.FindingSeverity.ERROR;

/** Copper of two nets nearer on a layer than either net's clearance — or touching. */
final class GapCheck implements PcbCheck.Ruled {

    @Override
    public List<PcbFinding> findings(CheckContext context, Map<String, Double> values) {
        PcbDrawing pcb = context.pcb();
        List<CopperPieces.Piece> pieces = context.pieces();
        NetRules rules = new NetRules(pcb, values);
        List<PcbFinding> findings = new ArrayList<>();
        Set<Long> compared = new HashSet<>();
        int unnamed = 0;
        double widest = Stream.concat(Stream.of(FindingWords.rule(values, PcbVocabulary.RULE_CLEARANCE)), pcb.netClasses().stream().map(NetClass::clearance))
                .mapToDouble(Double::doubleValue).max().orElse(0);
        for (String layer : pcb.copperLayerIds()) {
            List<Integer> onLayer = new ArrayList<>();
            for (int index = 0; index < pieces.size(); index++) {
                if (pieces.get(index).onLayer().test(layer)) {
                    onLayer.add(index);
                }
            }
            // Each piece measured against those whose boxes come within the widest gap of its own — never every other on the layer.
            BoxIndex onLayerNear = BoxTree.of(onLayer.stream().map(index -> pieces.get(index).shape().box()).toList());
            for (int first = 0; first < onLayer.size(); first++) {
                for (int second : onLayerNear.near(pieces.get(onLayer.get(first)).shape().box(), widest)) {
                    if (second <= first) {
                        continue;
                    }
                    int oneIndex = onLayer.get(first);
                    int otherIndex = onLayer.get(second);
                    CopperPieces.Piece one = pieces.get(oneIndex);
                    CopperPieces.Piece other = pieces.get(otherIndex);
                    if (one.group() == other.group() || (one.net().isPresent() && one.net().equals(other.net()))
                        || (one.part().isPresent() && one.part().equals(other.part())) || !one.shape().within(other.shape(), widest)
                        || !compared.add((long) oneIndex * pieces.size() + otherIndex)) {
                        continue;
                    }
                    double needed = Math.max(rules.clearanceOf(one.net()), rules.clearanceOf(other.net()));
                    CopperShape.Gap gap = one.shape().gapTo(other.shape());
                    if (gap.gap() >= needed - context.settings().tolerance()) {
                        continue;
                    }
                    if (findings.size() >= context.settings().namedGaps()) {
                        unnamed++;
                        continue;
                    }
                    String where = FindingWords.layerTitle(layer).toLowerCase(Locale.ROOT);
                    findings.add(new PcbFinding(ERROR, gap.gap() <= 0
                            ? FindingWords.named(one) + " touches " + FindingWords.named(other) + " on " + where
                            : FindingWords.named(one) + " is " + FindingWords.said(gap.gap()) + " from " + FindingWords.named(other) + " on " + where
                              + ": " + FindingWords.said(needed) + " is needed",
                            new ArrayList<>(new LinkedHashSet<>(List.of(one.key(), other.key()))), gap.at()));
                }
            }
        }
        if (unnamed > 0) {
            findings.add(new PcbFinding(ERROR, unnamed + " more gaps are too narrow", List.of()));
        }
        return findings;
    }
}
