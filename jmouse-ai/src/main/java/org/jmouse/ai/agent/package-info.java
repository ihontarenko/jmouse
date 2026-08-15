/**
 * Who acts on somebody's behalf, and through which client.
 *
 * <p>Two nouns, and separating them is the point of the package. An {@link org.jmouse.ai.agent.Agent}
 * is a persona a person created and can grant privileges to; an
 * {@link org.jmouse.ai.agent.AgentConnection} is one client holding a credential for it. Products that
 * had only the second discovered they had an identity that appeared when a client connected and vanished
 * when it disconnected — nothing to authorize, nothing to switch off, and nothing for a record to point
 * back at. Products that had only the first could not tell two clients apart or end one of them.
 *
 * <h2>No permission model lives here</h2>
 *
 * <p>Deliberately, and it is the design decision worth knowing before reading anything else. What an
 * agent may do belongs to whatever engine already authorizes the product's own endpoints, against the
 * same policy — a second model here would agree with the first until somebody changed one of them, and
 * then disagree silently. What this package supplies instead is the <em>shape</em> those privileges hang
 * off: an agent authorizes as itself and acts for its owner, which every access engine worth the name
 * already knows as a ceiling. {@link org.jmouse.ai.agent.AgentCallers} is that, in one line.
 *
 * <h2>The owner is an opaque reference, and nothing cascades</h2>
 *
 * <p>⚠️ A library table cannot carry a foreign key into a product's accounts without knowing what an
 * account is, so it does not try — the owner is a string this package never interprets, exactly as the
 * protocol modules' approved-subject reference already is. The price is real and is paid in one place:
 * {@link org.jmouse.ai.agent.AgentDirectory#discardAllOwnedBy(String)} has to be called wherever accounts
 * are deleted, or agents outlive their owners.
 *
 * <p>The foreign keys that <em>do</em> make sense point the other way, from a product's own tables into
 * these — which is how a record says which agent made it without this package learning what a record is.
 */
package org.jmouse.ai.agent;
