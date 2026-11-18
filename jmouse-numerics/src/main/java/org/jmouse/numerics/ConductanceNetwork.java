package org.jmouse.numerics;

import java.util.Arrays;

/**
 * Nodes joined by conductances — an electrical network, a thermal one — and the potentials a unit current sets up in it: one
 * ampere into a source node and out of a sink held at zero. Its nodal equations are a weighted graph Laplacian, symmetric and, with
 * the sink held, positive definite, so they are solved by conjugate gradients, preconditioned by each node's own conductance —
 * never factored: a network of thousands of nodes holds a handful of conductances a node, and stays that sparse while it is solved.
 * <p>
 * Nodes are numbered from 0 to {@code size - 1}. Not for two threads at once.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ConductanceNetwork {

    /**
     * When the solving stops: when the residual is below a tolerance, or after as many iterations as a network of its size is
     * given.
     *
     * @param tolerance         the residual — amperes left unbalanced — it is solved to
     * @param iterationsPerNode how many iterations it may take for each node
     * @param iterations        how many it may take besides
     */
    public record Stopping(double tolerance, int iterationsPerNode, int iterations) {
    }

    private final int      size;
    private final double[] diagonal;
    private int[]          froms = new int[16];
    private int[]          tos   = new int[16];
    private double[]       conductances = new double[16];
    private int            joined;

    /** A network of so many nodes, none joined yet. */
    public ConductanceNetwork(int size) {
        this.size = size;
        this.diagonal = new double[size];
    }

    public int size() {
        return size;
    }

    /** Two nodes joined by a conductance, beside whatever already joins them. */
    public void connect(int one, int other, double conductance) {
        if (joined == froms.length) {
            froms = Arrays.copyOf(froms, joined * 2);
            tos = Arrays.copyOf(tos, joined * 2);
            conductances = Arrays.copyOf(conductances, joined * 2);
        }
        froms[joined] = one;
        tos[joined] = other;
        conductances[joined] = conductance;
        joined++;
        diagonal[one] += conductance;
        diagonal[other] += conductance;
    }

    /**
     * Each node's potential with one ampere into {@code source} and out of {@code sink}, the sink held at zero: the source's is the
     * resistance between the two. Nodes no conductance reaches from the source are left at zero.
     */
    public double[] potentials(int source, int sink, Stopping stopping) {
        double[] solution = new double[size];
        double[] residual = new double[size];
        residual[source] = 1;
        double[] preconditioned = preconditioned(residual, sink);
        double[] direction = preconditioned.clone();
        double[] product = new double[size];
        double agreement = dot(residual, preconditioned);
        int most = stopping.iterationsPerNode() * size + stopping.iterations();
        for (int iteration = 0; iteration < most && Math.sqrt(dot(residual, residual)) > stopping.tolerance(); iteration++) {
            applied(direction, sink, product);
            double step = agreement / dot(direction, product);
            for (int node = 0; node < size; node++) {
                solution[node] += step * direction[node];
                residual[node] -= step * product[node];
            }
            preconditioned = preconditioned(residual, sink);
            double next = dot(residual, preconditioned);
            for (int node = 0; node < size; node++) {
                direction[node] = preconditioned[node] + next / agreement * direction[node];
            }
            agreement = next;
        }
        return solution;
    }

    /** The Laplacian, its sink's row and column held out, times a vector: into {@code product}. */
    private void applied(double[] vector, int sink, double[] product) {
        for (int node = 0; node < size; node++) {
            product[node] = node == sink ? 0 : diagonal[node] * vector[node];
        }
        for (int join = 0; join < joined; join++) {
            int one = froms[join];
            int other = tos[join];
            double conductance = conductances[join];
            if (one != sink && other != sink) {
                product[one] -= conductance * vector[other];
                product[other] -= conductance * vector[one];
            }
        }
    }

    /** A residual scaled by each node's own conductance: the Jacobi preconditioner, the sink held out. */
    private double[] preconditioned(double[] residual, int sink) {
        double[] scaled = new double[size];
        for (int node = 0; node < size; node++) {
            scaled[node] = node == sink || diagonal[node] == 0 ? 0 : residual[node] / diagonal[node];
        }
        return scaled;
    }

    private static double dot(double[] first, double[] second) {
        double sum = 0;
        for (int index = 0; index < first.length; index++) {
            sum += first[index] * second[index];
        }
        return sum;
    }
}
