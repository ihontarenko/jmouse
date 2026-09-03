# Search relevance: a shared scale, not normalisation

> The one decision in `jmouse-search` that is expensive to change later, taken before the first product
> was cut over. `JMF-275`; the argument that produced it is `XP-27`.

## The question

A federated search merges hits from several providers — forms, entries, files, pages, issues — into one
ranked list. For that list to mean anything, two providers' scores have to be comparable. There are two
ways to arrange that, and they cannot both be true.

**Normalise.** Every provider answers in `[0, 1]`. A provider divides by the total weight it applied, so
whatever it weighs, its best possible answer is `1.0`.

**Share the scale.** Weights come from one named set (`Weights.CRITICAL`, `PRIMARY`, …), contributions
sum, and nothing is divided. Two providers are comparable because they drew from the same numbers.

## The decision

**Share the scale.**

## Why

**Normalising punishes a provider for knowing more about its own rows.** Consider two providers, both
matching a title exactly:

| Provider | Weighs | Weighted sum | Normalised |
|---|---|---|---|
| a tag | `title` at `PRIMARY` | `4.0` | `4.0 / 4.0` = **1.00** |
| a page | `address` `CRITICAL`, `title` `PRIMARY`, `excerpt` `SECONDARY`, `body` `SUPPORTING` | `4.0` | `4.0 / 15.0` = **0.27** |

The page matched a title exactly and is ranked below a tag that did the same, because the page had more
fields to be asked about. Nothing in the answer explains it, and the fix a reader would reach for —
weighing fewer fields — makes the search worse.

**Summing has the failure the other way round, and it is the acceptable one.** A provider that weighs
many fields can accumulate a large score from many weak matches. That is a real effect and it is bounded:
each field contributes `weight × base` with `base ≤ 1`, the rungs are spaced by a factor of two
(`Weights`), and a weak match one rung down cannot overtake a strong match one rung up. A reader who
disagrees with a particular ranking can read the `FieldMatch` rows and see exactly which field paid for
it — which is not true of a normalised score, where the denominator is invisible.

**The scale is checkable and the normalisation is not.** "Everybody must use `Weights`" is a rule a
reviewer can see being broken at the call site. "Everybody must normalise by their own total" is a rule
whose violation looks identical to correct code.

## What it costs, stated plainly

- **A number means nothing on its own.** `5.4` is not "54% relevant". It is only ever comparable with
  other scores from the same query. Do not put it in an interface as a percentage, and do not compare one
  across releases — the rungs may move.
- **A provider that ignores `Weights` silently breaks the merge.** Its hits sort into the wrong places
  and nothing fails. This is the price of the decision, and the mitigation is that the weights are named
  constants rather than numbers, so a review sees `Weights.PRIMARY` and not `4.0`.
- **A foreign ranking has to be mapped onto the scale.** A database's full-text rank or a remote API's
  relevance is on nobody's scale. `MatchKind.EXTERNAL` marks it, and the provider asserts the mapping —
  the library cannot check it. It is a named factory rather than a silent default so that the assertion
  is visible.

## What would change the decision

A product where providers genuinely cannot agree on a weight vocabulary — where "the name of the thing"
means something different enough that `PRIMARY` is not one concept. If that turns up, the answer is not
normalisation; it is a per-directory scale, so the comparison stays explicit rather than becoming
arithmetic.

## Where it is already true

Kiwi's page search was built on this scale before the library existed (`KW-0112`), which is where the
weights were chosen and argued: the address is `CRITICAL` because somebody who types one is *naming* a
page rather than describing a subject; the body is `SUPPORTING` because a word in it may be the point of
the document or an aside in its fourteenth paragraph, and nothing in the text says which.
