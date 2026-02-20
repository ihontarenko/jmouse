package org.jmouse.dom;

public enum TagName {

    // sourceRoot tags
    HTML, HEAD, BODY, TITLE, BASE, LINK, META, STYLE, SCRIPT, NOSCRIPT,

    // content tags
    DIV, P, SPAN, H1, H2, H3, H4, H5, H6, HEADER, FOOTER, MAIN, SECTION, ARTICLE, ASIDE, DETAILS, MARK, NAV,

    // formatting tags
    B, I, U, STRONG, EM, SMALL, BIG, SUB, SUP, INS, DEL, CODE, KBD, SAMP, VAR, CITE, Q, BLOCKQUOTE, PRE, ABBR, ACRONYM,

    // media tags
    IMG, PICTURE, FIGURE, FIGCAPTION, AUDIO, VIDEO, SOURCE, TRACK,

    // lists
    UL, OL, LI, DL, DT, DD,

    // tables
    TABLE, CAPTION, THEAD, TBODY, TFOOT, TR, TH, TD, COL, COLGROUP,

    // forms
    FORM, INPUT, BUTTON, SELECT, OPTION, OPTGROUP, TEXTAREA, LABEL, FIELDSET, LEGEND, DATALIST, OUTPUT, PROGRESS, METER,

    // scripts
    CANVAS, SVG, OBJECT, EMBED, PARAM,

    // interactive tags
    A, AREA, MAP, SUMMARY, MENU, MENUITEM, DIALOG,

    // others
    IFRAME, BR, HR, WBR, TEMPLATE, COMMENT;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
