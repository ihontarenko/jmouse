package svit.scanner.filter.type;

public class AccessModifierClassFilter implements TypeFilter {

    private final int     modifiers;
    private final boolean invert;

    public AccessModifierClassFilter(int modifiers, boolean invert) {
        this.modifiers = modifiers;
        this.invert = invert;
    }

    public AccessModifierClassFilter(int modifiers) {
        this(modifiers, false);
    }

    @Override
    public boolean accept(Class<?> object) {
        return invert == ((object.getModifiers() & modifiers) == 0);
    }

}
