package org.jmouse.ai.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * How often one caller called one action, and how that call ended.
 *
 * <p><strong>Counters rather than rows.</strong> An operator asks "how much" and "how often refused,
 * and why", and both are answered by a number that only ever goes up. Deriving them from a trail
 * instead would make the answer depend on the trail's retention and — worse — would put a query over
 * somebody's records behind a screen that only ever needed totals.
 *
 * <p>⚠️ {@link #outcome} carries a verdict name, {@code FAILED}, or the name of the reason a call was
 * refused, in <strong>one</strong> column. That is the whole design: a refusal <em>rate</em> says
 * something is wrong and only the <em>distribution</em> says what. Many {@code AMBIGUOUS_SCOPE} means a
 * tool description is misleading a model about which scopes exist; many {@code UNPARSEABLE_VALUE} means
 * the domain's vocabulary is narrower than the model assumes; many {@code MISSING_PERMISSION} means
 * callers are being created with too little. One failure rate hides all three.
 *
 * <p>Reads are counted too. Volume is volume, and a caller hammering a listing in a loop is exactly
 * what an operator wants to see.
 *
 * <p>⚠️ <strong>This table is the library's.</strong> A product must add
 * {@code org.jmouse.ai.jpa.entity} to its entity scan — see the module's package javadoc for what
 * forgetting looks like.
 */
@Entity
@Table(name = AiToolCall.TABLE_NAME)
public class AiToolCall {

    /** Named once, so migrations, queries and any product's own view spell it the same way. */
    public static final String TABLE_NAME = "ai_tool_calls";

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "caller_id", length = 64, nullable = false, updatable = false)
    private String callerId;

    @Column(name = "tool_name", length = 64, nullable = false, updatable = false)
    private String toolName;

    @Column(name = "action_name", length = 64, nullable = false, updatable = false)
    private String actionName;

    @Column(name = "outcome", length = 64, nullable = false, updatable = false)
    private String outcome;

    @Column(name = "call_count", nullable = false)
    private long callCount;

    @Column(name = "last_called_at", nullable = false)
    private LocalDateTime lastCalledAt;

    protected AiToolCall() {
    }

    public AiToolCall(
            String id, String callerId, String toolName, String actionName, String outcome,
            long callCount, LocalDateTime lastCalledAt) {

        this.id           = id;
        this.callerId     = callerId;
        this.toolName     = toolName;
        this.actionName   = actionName;
        this.outcome      = outcome;
        this.callCount    = callCount;
        this.lastCalledAt = lastCalledAt;
    }

    public String getId() {
        return id;
    }

    public String getCallerId() {
        return callerId;
    }

    public String getToolName() {
        return toolName;
    }

    public String getActionName() {
        return actionName;
    }

    public String getOutcome() {
        return outcome;
    }

    public long getCallCount() {
        return callCount;
    }

    public LocalDateTime getLastCalledAt() {
        return lastCalledAt;
    }
}
