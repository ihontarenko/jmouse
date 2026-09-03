package org.jmouse.script.el.host;

import java.util.ArrayList;
import java.util.List;

/**
 * A host, invented for the tests, that happens to be a game.
 *
 * <h2>⚠️ Every noun in this file is the fixture's, not the language's</h2>
 *
 * <p>{@code world}, {@code player}, {@code mission}, {@code orders}, a unit, a tile, a mission — none of
 * them appear anywhere in {@code src/main/java}, and {@code NoGameNounsTest} exists to keep it that way.
 * They are here because a game exercises every construction the dialect has at once, which makes it the
 * cheapest honest end-to-end fixture there is. That is the whole reason it is a game and the whole
 * reason it is in test sources.</p>
 *
 * <p>Every method records the call and answers something canned, so a test can assert what a script
 * <em>did</em> rather than what it returned.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class FakeHost {

    /** Every facade call the scripts made, in order. */
    public final List<String> calls = new ArrayList<>();

    private boolean factoryUnlocked;
    private boolean scoutKilled;

    /**
     * Builds the catalogue this host declares — four facades and six events.
     *
     * @return the catalogue
     */
    public ScriptCatalogue catalogue() {
        return ScriptCatalogue.builder()
                .facade("world", new World())
                .facade("player", new Player())
                .facade("mission", new Mission())
                .facade("orders", new Orders())
                .event("start")
                .event("unload")
                .event("credits")
                .event("enter")
                .event("timer")
                .event("destroyed")
                .event("paused")
                .build();
    }

    private void record(String call) {
        calls.add(call);
    }

    /** The map and everything on it. */
    public final class World {

        public String point(String name) {
            return name;
        }

        public void reveal(String area) {
            record("world.reveal(" + area + ")");
        }

        /**
         * ⚠️ Two overloads and a varargs, on purpose — this is what a host actually writes, and until
         * JMF-269 none of it was reachable: the descriptor kept whichever was introspected last.
         */
        public void spawn(Object kind, Object where, Object owner) {
            record("world.spawn3(" + kind + ")");
        }

        public void spawn(Object kind, Object where, Object owner, Object tag) {
            record("world.spawn4(" + kind + ")");
        }

        public void enable(String what) {
            factoryUnlocked = true;
            record("world.enable(" + what + ")");
        }

        public boolean unlocked(String what) {
            return factoryUnlocked;
        }

        public boolean in_area(Object unit, String area) {
            return true;
        }

        public List<Unit> units_in(String area) {
            return List.of(new Unit("enemy", true, "scout", "scout"), new Unit("me", true, "tank", "tank"));
        }

        public Object nearest_resource(Object unit, String kind) {
            return "spice-field";
        }

        public boolean at(Object unit, Object place) {
            return true;
        }

        public boolean has_resource(Object unit) {
            return false;
        }

        public Object nearest_dropoff(Object unit) {
            return "refinery";
        }
    }

    /** Whoever is playing. */
    public final class Player {

        public boolean has(String what) {
            return true;
        }

        public int credits() {
            return 500;
        }

        public String id() {
            return "me";
        }
    }

    /** What this level is asking for. */
    public final class Mission {

        public void objective(String key, String text) {
            record("mission.objective(" + key + ")");
        }

        public void say(String text) {
            record("mission.say");
        }

        public void complete(String key) {
            scoutKilled = true;
            record("mission.complete(" + key + ")");
        }

        /** ⚠️ The bare overload the fixture always wanted. Unreachable until JMF-269. */
        public void complete() {
            record("mission.complete");
        }

        public boolean completed(String key) {
            return scoutKilled;
        }

        public void fail(String why) {
            record("mission.fail");
        }

        public String enemy() {
            return "enemy";
        }
    }

    /** What a unit is told to do. */
    public final class Orders {

        public void attack(Object unit) {
            record("orders.attack");
        }

        public void move(Object unit, Object where) {
            record("orders.move");
        }

        public void harvest(Object unit) {
            record("orders.harvest");
        }

        public void unload(Object unit) {
            record("orders.unload");
        }
    }

    /** Something on the map, as the host hands it to a handler. */
    public static final class Unit {

        private final String  owner;
        private final boolean alive;
        private final String  kind;
        private final String  tag;

        private String  state = "seek";
        private boolean full;
        private boolean empty = true;

        public Unit(String owner, boolean alive, String kind, String tag) {
            this.owner = owner;
            this.alive = alive;
            this.kind = kind;
            this.tag = tag;
        }

        public String getOwner() {
            return owner;
        }

        public boolean isAlive() {
            return alive;
        }

        public String getKind() {
            return kind;
        }

        public String getTag() {
            return tag;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public boolean isFull() {
            return full;
        }

        public void setFull(boolean full) {
            this.full = full;
        }

        public boolean isEmpty() {
            return empty;
        }

        public void setEmpty(boolean empty) {
            this.empty = empty;
        }
    }

    /** Something built, as the host hands it to a handler. */
    public record Building(String kind, String owner) {

        public String getKind() {
            return kind;
        }

        public String getOwner() {
            return owner;
        }
    }
}
