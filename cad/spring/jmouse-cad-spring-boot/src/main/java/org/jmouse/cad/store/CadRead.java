package org.jmouse.cad.store;

import org.jmouse.cad.store.problem.CadProblem;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A read of the CAD store, in a read-only transaction. A refusal it ends with — a document not found, not the reader's — changes
 * nothing, so it leaves the transaction of whoever asked as it was: an export that asks for a part's model, is refused, and goes on
 * without it, saying so, still finishes, where a refusal that marked the transaction for rollback would fail the whole export after
 * it was written.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Transactional(readOnly = true, noRollbackFor = CadProblem.class)
public @interface CadRead {
}
