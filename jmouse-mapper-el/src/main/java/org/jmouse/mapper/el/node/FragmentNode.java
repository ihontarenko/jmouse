package org.jmouse.mapper.el.node;

/**
 * A {@code fragment name { … }} — rules shared across targets, declared at file level.
 *
 * <p>A rule block that knows what it is called. The name has to travel with the node because a parser
 * returns one thing, and the block it produces is registered by the document under that name.</p>
 *
 * <p>⚠️ Flat by design: a fragment does not include another fragment, so a cycle cannot be written and
 * a resolution order never has to be defined.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class FragmentNode extends RuleBlockNode {

    private String name;

    /** @return what the fragment is called */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "fragment %s %s".formatted(name, super.toString());
    }
}
