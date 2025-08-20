package org.jmouse.web.security.firewall.policy;

import org.jmouse.web.http.HttpStatus;
import org.jmouse.web.security.firewall.InspectionPolicies;

/**
 * 🛡️ Firewall policy that protects against <b>SQL Injection</b> attacks.
 *
 * <p>SQL Injection is a critical vulnerability that occurs when untrusted input is
 * concatenated into SQL queries. Attackers may exploit this to manipulate database
 * queries, extract sensitive data, or even modify and delete records.</p>
 *
 * <h3>Inspection rules</h3>
 * <ul>
 *   <li>Uses configured {@link InspectionPolicies.InspectionGroup} to detect SQL injection attempts.</li>
 *   <li>Supports both <b>contains checks</b> (e.g., dangerous keywords like {@code '--'} or {@code '/*'})
 *       and <b>regex expressions</b> (e.g., {@code (?i)union\s+select}, {@code or 1=1}).</li>
 *   <li>If a suspicious SQL pattern is detected → request is blocked with the given {@link HttpStatus}
 *       and reason {@code "SQL INJECTION"}.</li>
 * </ul>
 *
 * <h3>Examples of detected payloads</h3>
 * <pre>
 *   ' OR 1=1 --
 *   UNION SELECT username, password FROM users
 *   SELECT * FROM accounts WHERE id=1; DROP TABLE accounts;
 * </pre>
 *
 * @see AbstractInspectionPolicy
 * @see InspectionPolicies
 */
public class SqlInjectionPolicy extends AbstractInspectionPolicy {

    /**
     * Creates an SQL Injection detection policy with the given inspection group and status.
     *
     * @param group  inspection rules for SQL injection detection (contains & regex patterns)
     * @param status HTTP status code to return when blocking the request
     */
    public SqlInjectionPolicy(InspectionPolicies.InspectionGroup group, HttpStatus status) {
        super(group, status, "SQL INJECTION");
    }

}
