package org.jmouse.cad.pcb.checks;

import org.jmouse.cad.pcb.PcbLayers;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.CopperPieces;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static org.jmouse.cad.pcb.geometry.CopperPieces.key;

/** How a finding says what it is about: lengths and numbers as a person reads them, copper by its name, things by their keys. */
final class FindingWords {

    private FindingWords() {
    }

    /** A length as a person reads it in a finding: to a thousandth, trailing zeros dropped, never below 0. */
    static String said(double millimetres) {
        return written(Math.max(0, rounded(millimetres, 3))) + " mm";
    }

    static double rounded(double value, int places) {
        return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }

    /** A number written as the interface writes one: no trailing zeros, no exponent. */
    static String written(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    /** A piece of copper by its label and its net. */
    static String named(CopperPieces.Piece piece) {
        return piece.net().map(net -> piece.label() + " (" + net + ")").orElse(piece.label());
    }

    /** A member of the joining by its label: its piece's, or its zone's. */
    static String labelOf(CopperNets.Member member, Optional<CopperPieces.Piece> piece) {
        return piece.map(CopperPieces.Piece::label).orElse("Zone " + (member.index() + 1));
    }

    /** What a copper layer is called in a finding: {@code Top copper}, {@code Inner copper 1}, {@code Bottom copper}. */
    static String layerTitle(String layer) {
        if (PcbLayers.isInnerCopper(layer)) {
            return "Inner copper " + layer.substring((PcbVocabulary.INNER_COPPER + PcbVocabulary.LAYER_JOINER).length());
        }
        return Character.toUpperCase(layer.charAt(0)) + layer.substring(1) + " copper";
    }

    /** A value the pcb is held to, by its rule word; 0 where it holds none. */
    static double rule(Map<String, Double> values, String word) {
        return values.getOrDefault(word, 0.0);
    }

    /** The keys of things of a kind, by their places. */
    static List<String> keys(String kind, List<Integer> indexes) {
        return indexes.stream().map(index -> key(kind, index)).toList();
    }

    /** The keys of things of a kind on a net, by the net each is on. */
    static List<String> keysOnNet(String kind, List<Optional<String>> nets, String net) {
        return indexesWhere(nets, found -> found.filter(net::equals).isPresent()).stream().map(index -> key(kind, index)).toList();
    }

    /** The places of the items a test lets through, in order. */
    static <T> List<Integer> indexesWhere(List<T> items, Predicate<T> wanted) {
        List<Integer> indexes = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            if (wanted.test(items.get(index))) {
                indexes.add(index);
            }
        }
        return indexes;
    }

    /** The place of the first item a test lets through; empty where none is. */
    static <T> Optional<Integer> firstIndexWhere(List<T> items, Predicate<T> wanted) {
        for (int index = 0; index < items.size(); index++) {
            if (wanted.test(items.get(index))) {
                return Optional.of(index);
            }
        }
        return Optional.empty();
    }
}
