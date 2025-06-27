package org.jmouse.util;

import java.util.List;
import java.util.Random;

public class IgnoreReasonGenerator extends AbstractStringIdGenerator {

    private static final List<String> REASONS = List.of(
            "🚫 Out of service due to laziness",
            "🧪 Experimental bean — not for public use",
            "🎩 Magic removed it. Don't ask.",
            "👻 Haunted by legacy code",
            "🚧 Under construction. Wear a helmet!",
            "🛸 Abducted by aliens (we think)",
            "🐢 Too slow for production",
            "🎭 It's just pretending to be a bean",
            "💤 Taking a nap, do not disturb",
            "🎉 Skipped for party reasons",
            "🥷 Hidden in shadows. Ninja bean.",
            "🪦 Deprecated and buried with honors",
            "📦 Misplaced in another package",
            "🫥 Nobody remembers why it exists",
            "🐉 Bean too powerful to register",
            "📡 Awaiting signal from HQ",
            "🪐 Orbiting another context",
            "🦄 Mythical bean, yet to be proven",
            "🐞 Bug-infested, do not approach",
            "🛑 Stopped for dramatic effect",
            "🎲 Skipped by roll of dice",
            "🧹 Swept under the rug",
            "🤖 Needs firmware update first",
            "🧊 On ice until further notice",
            "🍕 Went to get pizza, brb",
            "🚀 Launched and never came back",
            "🌪️ Swirled into context void",
            "🕵️‍♂️ Classified: top secret bean",
            "🪄 Turned into something else by a wizard",
            "🧼 Too clean to run in this environment",
            "🐾 Left mysterious tracks but no code",
            "🔕 Skipped to avoid waking sleeping threads"
    );

    private static final Random RANDOM = new Random();

    @Override
    public String generate() {
        return REASONS.get(RANDOM.nextInt(REASONS.size()));
    }

}
