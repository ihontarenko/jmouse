package org.jmouse.libre.pcb;

import java.util.Arrays;
import java.util.Optional;

/** The units a LibrePCB attribute is written in, by the word the format has for each, with the symbol a reader knows it by. */
public enum LibrePcbUnit {

    NONE("none", ""),
    MILLIOHM("milliohm", "mΩ"),
    OHM("ohm", "Ω"),
    KILOOHM("kiloohm", "kΩ"),
    MEGAOHM("megaohm", "MΩ"),
    PICOFARAD("picofarad", "pF"),
    NANOFARAD("nanofarad", "nF"),
    MICROFARAD("microfarad", "µF"),
    MILLIFARAD("millifarad", "mF"),
    FARAD("farad", "F"),
    NANOHENRY("nanohenry", "nH"),
    MICROHENRY("microhenry", "µH"),
    MILLIHENRY("millihenry", "mH"),
    HENRY("henry", "H"),
    NANOVOLT("nanovolt", "nV"),
    MICROVOLT("microvolt", "µV"),
    MILLIVOLT("millivolt", "mV"),
    VOLT("volt", "V"),
    KILOVOLT("kilovolt", "kV"),
    MEGAVOLT("megavolt", "MV"),
    NANOAMPERE("nanoampere", "nA"),
    MICROAMPERE("microampere", "µA"),
    MILLIAMPERE("milliampere", "mA"),
    AMPERE("ampere", "A"),
    KILOAMPERE("kiloampere", "kA"),
    MEGAAMPERE("megaampere", "MA"),
    NANOWATT("nanowatt", "nW"),
    MICROWATT("microwatt", "µW"),
    MILLIWATT("milliwatt", "mW"),
    WATT("watt", "W"),
    KILOWATT("kilowatt", "kW"),
    MEGAWATT("megawatt", "MW"),
    MICROHERTZ("microhertz", "µHz"),
    MILLIHERTZ("millihertz", "mHz"),
    HERTZ("hertz", "Hz"),
    KILOHERTZ("kilohertz", "kHz"),
    MEGAHERTZ("megahertz", "MHz"),
    GIGAHERTZ("gigahertz", "GHz");

    private final String word;
    private final String symbol;

    LibrePcbUnit(String word, String symbol) {
        this.word = word;
        this.symbol = symbol;
    }

    /** The unit a word of the format names; a unit a newer format has and this does not know is empty. */
    public static Optional<LibrePcbUnit> ofWord(String word) {
        return Arrays.stream(values()).filter(unit -> unit.word.equals(word)).findFirst();
    }

    public String symbol() {
        return symbol;
    }
}
