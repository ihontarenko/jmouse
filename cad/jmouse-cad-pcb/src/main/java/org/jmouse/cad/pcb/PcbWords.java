package org.jmouse.cad.pcb;

/**
 * The words the pcb and footprint lines are written with — each line's name and each of its clauses' —, said once: the
 * grammar ({@link PcbLinesParser}, {@link FootprintLinesParser}) reads them, the drawings read and write lines by them.
 * What a clause may hold — a layer, a shape — is the {@link PcbVocabulary}'s.
 */
public final class PcbWords {

    private PcbWords() {
    }

    public static final String ANCHOR = "anchor";
    public static final String ARC = "arc";
    public static final String AT = "at";
    public static final String BODY = "body";
    public static final String CLASS = "class";
    public static final String CLEARANCE = "clearance";
    public static final String COUNT = "count";
    public static final String CURVED = "curved";
    public static final String DIAMETER = "diameter";
    public static final String DIMENSION = "dimension";
    public static final String DRILL = "drill";
    public static final String FACE = "face";
    public static final String FIELD = "field";
    public static final String FILLED = "filled";
    public static final String FINISH = "finish";
    public static final String FOOTPRINT = "footprint";
    public static final String FORMATS = "formats";
    public static final String FROM = "from";
    public static final String GAP = "gap";
    public static final String HEIGHT = "height";
    public static final String HIDDEN = "hidden";
    public static final String HOLE = "hole";
    public static final String KEEPOUT = "keepout";
    public static final String LAYER = "layer";
    public static final String LAYERS = "layers";
    public static final String LENGTH = "length";
    public static final String MASK_MARGIN = "mask-margin";
    public static final String MODEL = "model";
    public static final String NAME = "name";
    public static final String NET = "net";
    public static final String NETS = "nets";
    public static final String NUMBER = "number";
    public static final String OFFSET = "offset";
    public static final String OPEN = "open";
    public static final String OPENS = "opens";
    public static final String OUTLINE = "outline";
    public static final String OUTPUT = "output";
    public static final String PAD = "pad";
    public static final String PAIR = "pair";
    public static final String PASTE_MARGIN = "paste-margin";
    public static final String PATH = "path";
    public static final String PLACE = "place";
    public static final String PRIORITY = "priority";
    public static final String PROFILE = "profile";
    public static final String REFERENCE = "reference";
    public static final String RING = "ring";
    public static final String ROLE = "role";
    public static final String ROTATE = "rotate";
    public static final String ROUNDING = "rounding";
    public static final String ROUNDNESS = "roundness";
    public static final String RULES = "rules";
    public static final String SCALE = "scale";
    public static final String SHAPE = "shape";
    public static final String SHOWN = "shown";
    public static final String SIDE = "side";
    public static final String SIZE = "size";
    public static final String SIZED = "sized";
    public static final String SIZES = "sizes";
    public static final String SKEW = "skew";
    public static final String SLUG = "slug";
    public static final String SUBJECT = "subject";
    public static final String TARGET = "target";
    public static final String TEARDROPS = "teardrops";
    public static final String TEXT = "text";
    public static final String THROUGH = "through";
    public static final String TITLE = "title";
    public static final String TO = "to";
    public static final String TRACK = "track";
    public static final String TYPE = "type";
    public static final String VIA = "via";
    public static final String WIDTH = "width";
    public static final String WIDTHS = "widths";
    public static final String WORDS = "words";
    public static final String ZONE = "zone";
}
