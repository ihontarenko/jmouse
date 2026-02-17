package org.jmouse.common.dom;

import org.jmouse.common.dom.node.ElementNode;
import org.jmouse.common.dom.node.TextNode;

import java.util.Map;

/**
 * A builder for constructing HTML-like node trees.
 * <p>
 * Supports all tags defined in {@link TagName} for building HTML structures programmatically.
 * <p>
 * Example:
 * <pre>{@code
 * NodeBuilder builder = NodeBuilder.create(TagName.HTML);
 * builder
 *     .head()
 *         .down()
 *             .title().withText("Example Page").up()
 *         .up()
 *     .body()
 *         .down()
 *             .div().withAttributes(Map.of("class", "container"))
 *                 .down()
 *                     .paragraph().withText("Welcome to the binder page.")
 *                 .up()
 *             .up()
 *         .up();
 *
 * Node sourceRoot = builder.build();
 * System.out.println(renderHtml(sourceRoot));
 * }</pre>
 */
public class HtmlBuilder {

    private final Node        node;
    private final HtmlBuilder parent;

    private HtmlBuilder(TagName tagName, HtmlBuilder parent) {
        this.node = new ElementNode(tagName);
        this.parent = parent;

        if (parent != null) {
            parent.node.append(this.node);
        }
    }

    public static HtmlBuilder create(TagName rootTag) {
        return new HtmlBuilder(rootTag, null);
    }

    public HtmlBuilder add(TagName tagName) {
        if (parent == null) {
            throw new IllegalStateException("Cannot add a sibling to the sourceRoot node.");
        }

        return parent.addChild(tagName);
    }

    public HtmlBuilder down() {
        return createChild();
    }

    public HtmlBuilder up() {
        return parent == null ? this : parent;
    }

    public HtmlBuilder withAttributes(Map<String, String> attributes) {
        attributes.forEach(node::addAttribute);
        return this;
    }

    public HtmlBuilder withText(String textContent) {
        TextNode textNode = new TextNode(textContent);
        node.append(textNode);
        return this;
    }

    public Node root() {
        HtmlBuilder rootBuilder = this;

        while (rootBuilder.parent != null) {
            rootBuilder = rootBuilder.parent;
        }

        return rootBuilder.node;
    }

    private HtmlBuilder createChild() {
        return new HtmlBuilder(node.getTagName(), this);
    }

    private HtmlBuilder addChild(TagName tagName) {
        return new HtmlBuilder(tagName, this);
    }

    // Dynamically generated methods for all tags in TagName
    public HtmlBuilder html() {
        return add(TagName.HTML);
    }

    public HtmlBuilder head() {
        return add(TagName.HEAD);
    }

    public HtmlBuilder body() {
        return add(TagName.BODY);
    }

    public HtmlBuilder title() {
        return add(TagName.TITLE);
    }

    public HtmlBuilder base() {
        return add(TagName.BASE);
    }

    public HtmlBuilder link() {
        return add(TagName.LINK);
    }

    public HtmlBuilder meta() {
        return add(TagName.META);
    }

    public HtmlBuilder style() {
        return add(TagName.STYLE);
    }

    public HtmlBuilder script() {
        return add(TagName.SCRIPT);
    }

    public HtmlBuilder noscript() {
        return add(TagName.NOSCRIPT);
    }

    public HtmlBuilder div() {
        return add(TagName.DIV);
    }

    public HtmlBuilder paragraph() {
        return add(TagName.P);
    }

    public HtmlBuilder span() {
        return add(TagName.SPAN);
    }

    public HtmlBuilder h1() {
        return add(TagName.H1);
    }

    public HtmlBuilder h2() {
        return add(TagName.H2);
    }

    public HtmlBuilder h3() {
        return add(TagName.H3);
    }

    public HtmlBuilder h4() {
        return add(TagName.H4);
    }

    public HtmlBuilder h5() {
        return add(TagName.H5);
    }

    public HtmlBuilder h6() {
        return add(TagName.H6);
    }

    public HtmlBuilder header() {
        return add(TagName.HEADER);
    }

    public HtmlBuilder footer() {
        return add(TagName.FOOTER);
    }

    public HtmlBuilder main() {
        return add(TagName.MAIN);
    }

    public HtmlBuilder section() {
        return add(TagName.SECTION);
    }

    public HtmlBuilder article() {
        return add(TagName.ARTICLE);
    }

    public HtmlBuilder aside() {
        return add(TagName.ASIDE);
    }

    public HtmlBuilder details() {
        return add(TagName.DETAILS);
    }

    public HtmlBuilder mark() {
        return add(TagName.MARK);
    }

    public HtmlBuilder nav() {
        return add(TagName.NAV);
    }

    public HtmlBuilder bold() {
        return add(TagName.B);
    }

    public HtmlBuilder italic() {
        return add(TagName.I);
    }

    public HtmlBuilder underline() {
        return add(TagName.U);
    }

    public HtmlBuilder strong() {
        return add(TagName.STRONG);
    }

    public HtmlBuilder emphasis() {
        return add(TagName.EM);
    }

    public HtmlBuilder small() {
        return add(TagName.SMALL);
    }

    public HtmlBuilder big() {
        return add(TagName.BIG);
    }

    public HtmlBuilder subscript() {
        return add(TagName.SUB);
    }

    public HtmlBuilder superscript() {
        return add(TagName.SUP);
    }

    public HtmlBuilder inserted() {
        return add(TagName.INS);
    }

    public HtmlBuilder deleted() {
        return add(TagName.DEL);
    }

    public HtmlBuilder code() {
        return add(TagName.CODE);
    }

    public HtmlBuilder keyboardInput() {
        return add(TagName.KBD);
    }

    public HtmlBuilder sampleOutput() {
        return add(TagName.SAMP);
    }

    public HtmlBuilder variable() {
        return add(TagName.VAR);
    }

    public HtmlBuilder citation() {
        return add(TagName.CITE);
    }

    public HtmlBuilder quote() {
        return add(TagName.Q);
    }

    public HtmlBuilder blockquote() {
        return add(TagName.BLOCKQUOTE);
    }

    public HtmlBuilder preformatted() {
        return add(TagName.PRE);
    }

    public HtmlBuilder abbreviation() {
        return add(TagName.ABBR);
    }

    public HtmlBuilder acronym() {
        return add(TagName.ACRONYM);
    }

    public HtmlBuilder image() {
        return add(TagName.IMG);
    }

    public HtmlBuilder picture() {
        return add(TagName.PICTURE);
    }

    public HtmlBuilder figure() {
        return add(TagName.FIGURE);
    }

    public HtmlBuilder figcaption() {
        return add(TagName.FIGCAPTION);
    }

    public HtmlBuilder audio() {
        return add(TagName.AUDIO);
    }

    public HtmlBuilder video() {
        return add(TagName.VIDEO);
    }

    public HtmlBuilder source() {
        return add(TagName.SOURCE);
    }

    public HtmlBuilder track() {
        return add(TagName.TRACK);
    }

    public HtmlBuilder unorderedList() {
        return add(TagName.UL);
    }

    public HtmlBuilder orderedList() {
        return add(TagName.OL);
    }

    public HtmlBuilder listItem() {
        return add(TagName.LI);
    }

    public HtmlBuilder descriptionList() {
        return add(TagName.DL);
    }

    public HtmlBuilder descriptionTerm() {
        return add(TagName.DT);
    }

    public HtmlBuilder descriptionDetails() {
        return add(TagName.DD);
    }

    public HtmlBuilder table() {
        return add(TagName.TABLE);
    }

    public HtmlBuilder caption() {
        return add(TagName.CAPTION);
    }

    public HtmlBuilder tableHead() {
        return add(TagName.THEAD);
    }

    public HtmlBuilder tableBody() {
        return add(TagName.TBODY);
    }

    public HtmlBuilder tableFooter() {
        return add(TagName.TFOOT);
    }

    public HtmlBuilder tableRow() {
        return add(TagName.TR);
    }

    public HtmlBuilder tableHeader() {
        return add(TagName.TH);
    }

    public HtmlBuilder tableData() {
        return add(TagName.TD);
    }

    public HtmlBuilder col() {
        return add(TagName.COL);
    }

    public HtmlBuilder colgroup() {
        return add(TagName.COLGROUP);
    }

    public HtmlBuilder form() {
        return add(TagName.FORM);
    }

    public HtmlBuilder input() {
        return add(TagName.INPUT);
    }

    public HtmlBuilder button() {
        return add(TagName.BUTTON);
    }

    public HtmlBuilder select() {
        return add(TagName.SELECT);
    }

    public HtmlBuilder option() {
        return add(TagName.OPTION);
    }

    public HtmlBuilder optgroup() {
        return add(TagName.OPTGROUP);
    }

    public HtmlBuilder textarea() {
        return add(TagName.TEXTAREA);
    }

    public HtmlBuilder label() {
        return add(TagName.LABEL);
    }

    public HtmlBuilder fieldset() {
        return add(TagName.FIELDSET);
    }

    public HtmlBuilder legend() {
        return add(TagName.LEGEND);
    }

    public HtmlBuilder datalist() {
        return add(TagName.DATALIST);
    }

    public HtmlBuilder output() {
        return add(TagName.OUTPUT);
    }

    public HtmlBuilder progress() {
        return add(TagName.PROGRESS);
    }

    public HtmlBuilder meter() {
        return add(TagName.METER);
    }

    public HtmlBuilder canvas() {
        return add(TagName.CANVAS);
    }

    public HtmlBuilder svg() {
        return add(TagName.SVG);
    }

    public HtmlBuilder object() {
        return add(TagName.OBJECT);
    }

    public HtmlBuilder embed() {
        return add(TagName.EMBED);
    }

    public HtmlBuilder param() {
        return add(TagName.PARAM);
    }

    public HtmlBuilder map() {
        return add(TagName.MAP);
    }

    public HtmlBuilder area() {
        return add(TagName.AREA);
    }

    public HtmlBuilder summary() {
        return add(TagName.SUMMARY);
    }

    public HtmlBuilder menu() {
        return add(TagName.MENU);
    }

    public HtmlBuilder menuitem() {
        return add(TagName.MENUITEM);
    }

    public HtmlBuilder dialog() {
        return add(TagName.DIALOG);
    }

    public HtmlBuilder iframe() {
        return add(TagName.IFRAME);
    }

    public HtmlBuilder breakLine() {
        return add(TagName.BR);
    }

    public HtmlBuilder horizontalRule() {
        return add(TagName.HR);
    }

    public HtmlBuilder wordBreak() {
        return add(TagName.WBR);
    }

    public HtmlBuilder template() {
        return add(TagName.TEMPLATE);
    }

    public HtmlBuilder comment() {
        return add(TagName.COMMENT);
    }
}

