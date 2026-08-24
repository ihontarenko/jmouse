package org.jmouse.query.sql.smoke.demo;

/**
 * All ten, in order — one entry point each, one database between them.
 *
 * <pre>
 *   mvn -pl jmouse-query-sql exec:java -Dexec.mainClass=org.jmouse.query.sql.smoke.demo.DemoSmoke
 * </pre>
 *
 * <p>⚠️ <strong>Demonstrations, not tests.</strong> They talk to a live database and print what they
 * find, because the point is to show a query, the SQL it became, the values it bound and the rows that
 * came back — which an assertion hides. The tally at the end counts statements that ran and refusals
 * that refused; a red line is worth reading, not worth trusting on its own.</p>
 *
 * <table>
 *   <caption>What each one is about</caption>
 *   <tr><th>1</th><td>{@link ClinicDemo}</td><td>a URL — {@code ?jmq:filter=} and {@code ?jmq:order=}</td></tr>
 *   <tr><th>2</th><td>{@link LogisticsDemo}</td><td>an annotation on a repository method</td></tr>
 *   <tr><th>3</th><td>{@link SensorsDemo}</td><td>a YAML alert rule</td></tr>
 *   <tr><th>4</th><td>{@link BillingDemo}</td><td>a {@code .jmq} file of functions, and a view calling them</td></tr>
 *   <tr><th>5</th><td>{@link FunnelDemo}</td><td>a dashboard tile — aggregation</td></tr>
 *   <tr><th>6</th><td>{@link HiringDemo}</td><td>an agent over MCP, correcting itself from a refusal</td></tr>
 *   <tr><th>7</th><td>{@link ShopDemo}</td><td>an export — a projection that computes, over a bag</td></tr>
 *   <tr><th>8</th><td>{@link PipelineDemo}</td><td>one line in a terminal</td></tr>
 *   <tr><th>9</th><td>{@link CourseDemo}</td><td>a live block embedded in a page</td></tr>
 *   <tr><th>10</th><td>{@link TenantDemo}</td><td>row-level scoping composed in Java</td></tr>
 * </table>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class DemoSmoke {

    private DemoSmoke() {
    }

    public static void main(String[] arguments) {
        ClinicDemo.run();
        LogisticsDemo.run();
        SensorsDemo.run();
        BillingDemo.run();
        FunnelDemo.run();
        HiringDemo.run();
        ShopDemo.run();
        PipelineDemo.run();
        CourseDemo.run();
        TenantDemo.run();

        Demo.summary();
    }
}
