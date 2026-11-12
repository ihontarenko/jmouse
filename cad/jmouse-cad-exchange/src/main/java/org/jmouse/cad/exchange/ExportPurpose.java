package org.jmouse.cad.exchange;

/**
 * What a format's files are for, which decides what is asked of a document before it is written: what a factory makes a
 * pcb from is refused while the pcb's checks find an error; what another program goes on working with, or a person reads,
 * is written as the document stands.
 */
public enum ExportPurpose {

    /** Files a factory makes from: Gerber, IPC-2581, ODB++, a pcb house's set. */
    FABRICATION,

    /** Files another program goes on working with: a KiCad project, a SPICE deck, a 3D model. */
    EXCHANGE,

    /** Files a person reads: drawings, a bill of materials. */
    DOCUMENTATION
}
