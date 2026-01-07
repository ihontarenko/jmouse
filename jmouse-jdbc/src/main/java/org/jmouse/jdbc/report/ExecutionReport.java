package org.jmouse.jdbc.report;

public sealed interface ExecutionReport
        permits QueryReport, UpdateReport, BatchReport, CallReport {
}
