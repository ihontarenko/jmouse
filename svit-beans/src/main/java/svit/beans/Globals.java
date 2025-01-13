package svit.beans;

public enum Globals implements Scope {

    STATE_A, STATE_B;

    private static final ThreadLocal<Scope> THREAD_LOCAL = ThreadLocal.withInitial(() -> STATE_A);

    public static void set(Scope value) {
        THREAD_LOCAL.set(value);
    }

    private static Scope get() {
        return THREAD_LOCAL.get();
    }

    @Override
    public int id() {
        return ordinal() + 13 * 1000;
    }
}
