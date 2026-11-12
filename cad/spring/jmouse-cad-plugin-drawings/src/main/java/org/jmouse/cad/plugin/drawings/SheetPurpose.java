package org.jmouse.cad.plugin.drawings;

/** What a sheet is for, which says how it is framed and where it goes. */
enum SheetPurpose {

    /** The document's front: no frame, no title block. */
    COVER,

    /** Read in the document alone. */
    DOCUMENT,

    /** Read in the document, and sent as a DXF of its own: what a factory draws from. */
    DRAWING
}
