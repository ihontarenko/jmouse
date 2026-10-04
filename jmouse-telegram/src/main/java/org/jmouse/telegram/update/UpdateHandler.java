package org.jmouse.telegram.update;

/**
 * What a product does with an update.
 *
 * <p>⚠️ Returns nothing and consumes nothing. A handler does not decide whether other handlers run —
 * see {@link UpdateDispatcher}, where fan-out is the point: two products may legitimately care about
 * the same button press, and a handler that could swallow an update would make which one wins depend
 * on registration order.
 *
 * <p>⚠️ A handler that throws is isolated by the dispatcher rather than taking the ingestion loop with
 * it. That is a safety net and not a licence: an exception here means one update was dropped, silently
 * as far as Telegram is concerned, because Telegram considers a delivered update delivered.
 */
@FunctionalInterface
public interface UpdateHandler {

    void handle(Update update);
}
