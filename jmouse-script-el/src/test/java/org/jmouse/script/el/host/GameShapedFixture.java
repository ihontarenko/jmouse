package org.jmouse.script.el.host;

/**
 * The fixture the dialect is judged against, and the second file it composes with.
 *
 * <h2>⚠️ Two edits from the version this ticket was written with</h2>
 *
 * <p><strong>{@code not} became {@code !}.</strong> {@code not} is already this engine's second spelling
 * of {@code !=} and the partner of {@code is} ({@code x is not null}); admitting it as a prefix would
 * fork the expression language, which is the one thing the parser ticket exists to prevent. jmouse-el
 * has had unary {@code !} all along.</p>
 *
 * <p><strong>{@code hostile} moved into the included file.</strong> It was declared in the same block
 * that called it, so {@code include} was decorative — the fixture would have passed with the second
 * file missing entirely. It is the only helper either file shares, so moving it is what makes
 * composition something this suite actually proves.</p>
 *
 *
 * <p>⚠️ <strong>A third edit was made and then UNDONE.</strong> The fixture spawns with three arguments
 * in one place and four in another, and writes {@code complete(key)} beside a bare {@code complete()} —
 * both of which were unreachable, because a facade method was resolved by name alone. That was worked
 * around here by flattening the arities; JMF-269 fixed the engine instead, and the fixture is back to
 * the text the ticket carried. The same is true of the third {@code destroyed} handler, which reads
 * {@code building.kind} on an event that carries only a unit — JMF-270.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
final class GameShapedFixture {

    /** What {@code include 'common.jms'} pulls in. */
    static final String COMMON = """
            # common.jms — what every mission shares.

            script "common" {

                function hostile(unit)
                    return unit.owner != @player.id() and unit.alive
                end
            }
            """;

    /** The mission. Every construction the grammar has, in the shape a real one would be written. */
    static final String SLICE = """
            # jMS — jMouse Script. File extension .jms
            #
            # Expressions are jmouse-el as it already is, including scoped calls:
            #   @bean.methodName('argument')
            #   @bean#CONST
            #   @bean:$field
            # The host exposes a closed catalogue of facades (world, player, mission, orders),
            # not the whole DI container.
            #
            # Compile once at load → cached node tree. Events evaluate nodes.

            include 'common.jms'

            script "slice-01" {

                # ── helpers: a Lua function, jME + @facade.method inside ──────────────

                function yard_ready()
                    return @player.has('refinery') and @player.credits() >= 200
                end

                # ── mission start ─────────────────────────────────────────────────────

                on start do
                    @world.reveal('player_base')
                    @mission.objective('unload-once', "Return the harvester with spice")
                    @mission.objective('kill-scout',  "Destroy the marked scout")
                    @world.spawn('harvester', @world.point('player_spawn'), @player.id())
                    @world.spawn('scout',     @world.point('ridge'),        @mission.enemy(), 'scout')
                end

                # ── first unload unlocks production ───────────────────────────────────

                on unload when building.kind == 'dropoff' do
                    local n = count

                    if n == 1 then
                        @world.enable('factory')
                        @mission.say("the yard is open — build a tank")
                        @mission.complete('unload-once')
                    elseif n > 10 then
                        @mission.say("spice flows")
                    end
                end

                # ── credits as a gate ─────────────────────────────────────────────────

                on credits when yard_ready() do
                    if !@world.unlocked('factory') then
                        @world.enable('factory')
                    end
                end

                # ── area: hostiles on the ridge ───────────────────────────────────────

                on enter when @world.in_area(unit, 'ridge') do
                    for u in @world.units_in('ridge') do
                        if hostile(u) then
                            @orders.attack(u)
                        end
                    end
                end

                # ── timer fail ────────────────────────────────────────────────────────

                on timer 180 do
                    if !@mission.completed('kill-scout') then
                        @mission.fail("the scout got away")
                    end
                end

                # ── combat win / losses ───────────────────────────────────────────────

                on destroyed when unit.tag == 'scout' do
                    @mission.complete('kill-scout')
                    @mission.complete()
                end

                on destroyed when unit.kind == 'harvester' and unit.owner == @player.id() do
                    @mission.fail("harvester lost")
                end

                on destroyed when building.kind == 'dropoff' and building.owner == @player.id() do
                    @mission.fail("refinery destroyed")
                end
            }

            # Mechanic: YAML keeps hp/speed. Behaviour is Lua-shaped if/elseif on unit.state
            # plus @orders / @world — the same jME calls as the story half. No arrow machine.

            behaviour "gatherer" do

                function tick(unit)
                    if unit.state == 'seek' then
                        local spice = @world.nearest_resource(unit, 'spice')
                        if spice == null then
                            return
                        end
                        if @world.at(unit, spice) then
                            unit.state = 'harvest'
                        else
                            @orders.move(unit, spice)
                        end

                    elseif unit.state == 'harvest' then
                        if unit.full or !@world.has_resource(unit) then
                            unit.state = 'return'
                        else
                            @orders.harvest(unit)
                        end

                    elseif unit.state == 'return' then
                        local drop = @world.nearest_dropoff(unit)
                        if @world.at(unit, drop) then
                            unit.state = 'unload'
                        else
                            @orders.move(unit, drop)
                        end

                    elseif unit.state == 'unload' then
                        @orders.unload(unit)
                        if unit.empty then
                            unit.state = 'seek'
                        end
                    end
                end

            end
            """;

    private GameShapedFixture() {
    }
}
