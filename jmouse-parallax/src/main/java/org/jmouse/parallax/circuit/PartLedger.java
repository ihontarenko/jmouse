package org.jmouse.parallax.circuit;

import java.util.Arrays;

/**
 * What every part of a piece took over the accepted steps — its energy, and for a part that warms the heat its law is
 * advanced by. Each device's power is read once per accepted step and serves both: a resistor's power read twice per
 * step, once for its heat and once for its energy, was a fifth of a rectifier's run.
 *
 * <p>Heat moves in seconds. On a realistic bench, whose equations read a part's own temperature, every law is advanced
 * once {@link #HEAT_STRETCH} of circuit time has gathered; on an ideal one only when it is read ({@link #advanceHeat}) —
 * it is shown there, and read by nothing.</p>
 */
final class PartLedger {

    /**
     * How much circuit time a part's heat gathers before its law is advanced on a realistic bench: a tenth of a
     * millisecond, a hundred-thousandth of the shortest thermal time constant in the catalogue (a second).
     */
    private static final double HEAT_STRETCH = 1e-4;

    private final PartIndex parts;
    /** The devices it reads — every device of a part, and every one that warms —, by place among the devices. */
    private final int[]     read;
    /** Each read device's power at the end of the last accepted step, NaN before the first. */
    private final double[]  lastPower;
    /** Each read device's heat, or null; and the energy it took since its heat was last advanced. */
    private final PartHeat[] heats;
    private final double[]  heatEnergy;
    private double          heatTime;
    /** Whether a part's own temperature feeds back into its equations: a realistic bench. */
    private final boolean   heatFeedsBack;
    /** Whether a part's energy is kept — a screen reads it —, and each part's since it was last read. */
    private boolean         keepsEnergy = true;
    private final double[]  energy;
    private double          energyTime;
    private final boolean   warms;

    PartLedger(PartIndex parts, int deviceCount, boolean heatFeedsBack) {
        this.parts = parts;
        this.heatFeedsBack = heatFeedsBack;
        int[] read = new int[deviceCount];
        int count = 0;
        boolean warms = false;
        for (int device = 0; device < deviceCount; device++) {
            boolean warming = parts.device(device).heat() != null;
            warms |= warming;
            if (parts.partOf(device) >= 0 || warming) {
                read[count++] = device;
            }
        }
        this.read = Arrays.copyOf(read, count);
        this.warms = warms;
        this.lastPower = new double[count];
        Arrays.fill(lastPower, Double.NaN);
        this.heats = new PartHeat[count];
        for (int index = 0; index < count; index++) {
            heats[index] = parts.device(this.read[index]).heat();
        }
        this.heatEnergy = new double[count];
        this.energy = new double[parts.count()];
    }

    /** Keeps no energy from now on: a run that reads only the solution does not pay for it. */
    void keepNoEnergy() {
        keepsEnergy = false;
    }

    /** Whether anything is gathered from an accepted step at all. */
    boolean gathers() {
        return keepsEnergy || warms && heatFeedsBack;
    }

    /**
     * One accepted step: each device's power at its end, into its heat and its part's energy. A step that lands on an
     * edge held the level before the jump all along: its energy is the power before it.
     *
     * @param before the state the step started from, read when no power was known at its start
     */
    void accepted(double[] solution, double at, double duration, boolean landing, double[] before, double beforeTime) {
        for (int index = 0; index < read.length; index++) {
            Device device = parts.device(read[index]);
            boolean heated = heats[index] != null && (heatFeedsBack || keepsEnergy);
            int part = parts.partOf(read[index]);
            boolean counted = keepsEnergy && part >= 0;
            if (!heated && !counted) {
                continue;
            }
            double power = device.power(solution, at);
            if (heated) {
                heatEnergy[index] += power * duration;
            }
            if (counted) {
                double held = !landing ? power : Double.isNaN(lastPower[index]) ? device.power(before, beforeTime) : lastPower[index];
                energy[part] += held * duration;
            }
            lastPower[index] = power;
        }
        if (keepsEnergy) {
            energyTime += duration;
        }
        if (warms && (heatFeedsBack || keepsEnergy)) {
            heatTime += duration;
            if (heatFeedsBack && heatTime >= HEAT_STRETCH) {
                advanceHeat();
            }
        }
    }

    /** Every heat law advanced at the average power its part took since it was last advanced. */
    void advanceHeat() {
        if (heatTime > 0) {
            for (int index = 0; index < read.length; index++) {
                if (heats[index] != null) {
                    heats[index].advance(heatEnergy[index] / heatTime, heatTime);
                }
            }
        }
        Arrays.fill(heatEnergy, 0);
        heatTime = 0;
    }

    /** Each part's average power since this was last asked, watts, by the part's place; and a new stretch begun. */
    double[] takePowers() {
        double[] powers = new double[energy.length];
        for (int part = 0; part < energy.length; part++) {
            powers[part] = energyTime > 0 ? energy[part] / energyTime : 0;
        }
        Arrays.fill(energy, 0);
        energyTime = 0;
        return powers;
    }

    /** Goes on as warm as {@code previous} left each device: the same netlist builds its devices in the same order. */
    void continueFrom(PartLedger previous) {
        int count = Math.min(read.length, previous.read.length);
        for (int index = 0; index < count; index++) {
            if (heats[index] != null && previous.heats[index] != null) {
                heats[index].continueFrom(previous.heats[index]);
            }
        }
    }
}
