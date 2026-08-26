package org.jmouse.liveblocks.web;

/**
 * What a picker is asking: one namespace, and however much has been typed so far.
 *
 * <p>⚠️ <strong>One namespace per request, unlike {@code resolve}.</strong> Resolving batches because a
 * document names everything at once and a page must not cost twenty round trips. A picker is the
 * opposite shape — a person is looking at one tab, typing, and the request is re-sent on every
 * keystroke. Batching every namespace into that would ask four products a question three of them are not
 * being shown the answer to.
 *
 * @param namespace which kind of thing — {@code issue}, {@code part}
 * @param query     what has been typed. ⚠️ Blank is legitimate and means "the first few"
 * @param limit     the most to return; clamped by {@link DirectiveResolveController}
 */
public record SuggestDirectivesRequest(String namespace, String query, int limit) {
}
