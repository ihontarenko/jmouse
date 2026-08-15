package org.jmouse.ai.management;

import org.jmouse.ai.view.UsageTotals;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * How much has been used, by whom, and how much of it was refused.
 *
 * <p>The outcome stays in the grain rather than being summed away, and that is what makes this screen
 * worth opening: a caller whose calls are ninety per cent {@code MISSING_PERMISSION} is the single most
 * useful thing this module reports, and a total per caller would hide exactly that.
 *
 * <p>The two filters are applied by the port rather than here, so a product whose implementation can
 * push them into a query does, and one that cannot still answers.
 */
@RestController
@RequestMapping(ManagementRoutes.PREFIX)
public class UsageController {

    private final UsageTotals usage;

    public UsageController(UsageTotals usage) {
        this.usage = usage;
    }

    /**
     * Counted calls, busiest first.
     *
     * <p>⚠️ Both narrowings apply where both are given. Answering one and silently dropping the other
     * would return more rows than were asked for, which a screen renders without complaint and a person
     * reads as the answer to the question they typed.
     *
     * @param caller narrows to one caller
     * @param action narrows to one action, named as {@code parts.discard} — the dotted spelling the
     *               trail records, not the wire name
     */
    @GetMapping("/usage")
    public List<UsageTotals.Total> usage(
            @RequestParam(name = "caller", required = false) String caller,
            @RequestParam(name = "action", required = false) String action) {

        // The port is asked first wherever it can be, so an implementation that pushes the narrowing
        // into a query does; whatever is left is filtered here.
        List<UsageTotals.Total> counted = named(caller)
                ? usage.forCaller(caller)
                : named(action) ? usage.forAction(action) : usage.all();

        if (named(caller) && named(action)) {
            return counted.stream()
                    .filter(total -> total.qualifiedName().equals(action))
                    .toList();
        }

        return counted;
    }

    private static boolean named(String value) {
        return value != null && !value.isBlank();
    }
}
