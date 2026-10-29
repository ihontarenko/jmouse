package org.jmouse.cad.simulation.elaboration;

import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.component.Equation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A circuit cut into the parts that do not affect one another, so each can be solved on its own, with its own step.
 *
 * <p>The cut is exact, not an approximation. A net an ideal voltage source holds against ground has that source's
 * voltage whatever is connected to it — so the parts that meet only there, and at ground, are independent: what one
 * draws from the net changes nothing the other sees. Each piece gets its own copy of every such source it touches.</p>
 *
 * <p>Why it matters: a circuit is solved with one time step for all of it, and every edge anywhere shortens that step
 * everywhere. Ten timers on one supply cost ten edges' worth of small steps for each of them — a hundred times one
 * timer, where they should cost ten. A global-step simulator (SPICE among them) pays that; split, it is linear.</p>
 */
public final class CircuitPartitions {

    /** What a voltage equation is, of the two a generator's drive can be. */
    private static final String VOLTAGE = "voltage";

    private CircuitPartitions() {
    }

    /** One member of a circuit — a part, or an element the bench put in —, and the nets it joins. */
    private record Member(ElaboratedPart part, EngineElement element) {

        List<String> nets() {
            return part != null ? List.copyOf(part.connections().values()) : List.of(element.positive(), element.negative());
        }
    }

    /** The pieces of the circuit — the circuit itself when it does not split. */
    public static List<ElaboratedCircuit> of(ElaboratedCircuit elaborated) {
        List<Member> members = new ArrayList<>();
        elaborated.parts().forEach(part -> members.add(new Member(part, null)));
        elaborated.elements().forEach(element -> members.add(new Member(null, element)));
        Map<String, Member> holders = new LinkedHashMap<>();
        for (Member member : members) {
            String held = member.part() == null ? null : heldNet(member.part());
            if (held != null) {
                holders.putIfAbsent(held, member);
            }
        }
        if (holders.isEmpty()) {
            return List.of(elaborated);
        }

        // Union of the nets every other member joins; held nets and ground join nothing.
        Map<String, String> parent = new HashMap<>();
        Set<Member> sources = new HashSet<>(holders.values());
        List<Member> others = members.stream().filter(member -> !sources.contains(member)).toList();
        for (Member member : others) {
            List<String> joined = member.nets().stream().filter(net -> !Circuit.GROUND.equals(net) && !holders.containsKey(net)).toList();
            for (int index = 1; index < joined.size(); index++) {
                union(parent, joined.getFirst(), joined.get(index));
            }
            joined.forEach(net -> find(parent, net));
        }

        Map<String, List<Member>> groups = new LinkedHashMap<>();
        List<Member> loose = new ArrayList<>();
        for (Member member : others) {
            String net = member.nets().stream().filter(candidate -> !Circuit.GROUND.equals(candidate) && !holders.containsKey(candidate))
                    .findFirst().orElse(null);
            if (net == null) {
                loose.add(member);
            } else {
                groups.computeIfAbsent(find(parent, net), ignored -> new ArrayList<>()).add(member);
            }
        }
        if (groups.size() <= 1) {
            return List.of(elaborated);
        }
        // A member only between held nets and ground (a load across the supply) belongs to the first piece.
        groups.values().iterator().next().addAll(loose);

        List<ElaboratedCircuit> split = new ArrayList<>();
        for (List<Member> group : groups.values()) {
            Set<String> touched = new LinkedHashSet<>();
            group.forEach(member -> touched.addAll(member.nets()));
            List<Member> piece = new ArrayList<>();
            holders.forEach((net, source) -> {
                if (touched.contains(net)) {
                    piece.add(source);
                }
            });
            piece.addAll(group);
            split.add(elaborated.piece(piece.stream().map(Member::part).filter(part -> part != null).toList(),
                    piece.stream().map(Member::element).filter(element -> element != null).toList()));
        }
        return split;
    }

    /**
     * The net an ideal voltage source holds against ground — a generator whose drive is a voltage —, or null for anything
     * else. A current source drives a net and holds nothing.
     */
    private static String heldNet(ElaboratedPart part) {
        Optional<Equation.Branch> drive = part.component().generated();
        if (drive.isEmpty() || !drive.get().kind().equals(VOLTAGE)) {
            return null;
        }
        String positive = part.connections().get(drive.get().from());
        String negative = part.connections().get(drive.get().to());
        if (Circuit.GROUND.equals(negative) && positive != null && !Circuit.GROUND.equals(positive)) {
            return positive;
        }
        if (Circuit.GROUND.equals(positive) && negative != null && !Circuit.GROUND.equals(negative)) {
            return negative;
        }
        return null;
    }

    private static String find(Map<String, String> parent, String net) {
        parent.putIfAbsent(net, net);
        String root = parent.get(net);
        if (!root.equals(net)) {
            root = find(parent, root);
            parent.put(net, root);
        }
        return root;
    }

    private static void union(Map<String, String> parent, String first, String second) {
        String left = find(parent, first);
        String right = find(parent, second);
        if (!left.equals(right)) {
            parent.put(right, left);
        }
    }
}
