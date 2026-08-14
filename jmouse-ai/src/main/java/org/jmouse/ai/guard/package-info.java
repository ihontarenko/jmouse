/**
 * Everything that stands between a language model and a caller's data once the call is already
 * permitted.
 *
 * <p>A guard is not an authorizer. Authorization asks whether this caller may do this at all; a guard
 * asks whether <em>this particular call</em> should proceed even though they may. Each addresses a
 * different way an assistant goes wrong, and each is independently sufficient to stop the failure it
 * addresses: a loop quietly producing hundreds of valid calls, one call reaching far more records than
 * anyone intended, a wrong intent acted on before anyone saw it, and the same call arriving twice.
 *
 * <p>Two distinctions the chain depends on and which are easy to lose. <strong>Confirmation asks
 * whether you meant it; the ceiling says an operation that size is not available at all</strong> — no
 * amount of agreeing gets past a ceiling, which is exactly why it is not merely a larger threshold.
 * And a delete matching nothing is a mistake rather than a no-op: previewing zero records reads to a
 * model as "ready to proceed" and to a person as "it worked", immediately before nothing happens for
 * the wrong reason.
 *
 * <p>The rate limit applies to every call; the rest only to writes. A loop is a loop whether or not it
 * changes anything, while a read has no blast radius to bound, nothing to preview and nothing to
 * duplicate.
 *
 * <p>Order matters and is argued for on the chain itself. A product contributes a fifth guard as a
 * bean; a product wanting none configures an empty chain and the dispatcher still works.
 */
package org.jmouse.ai.guard;
