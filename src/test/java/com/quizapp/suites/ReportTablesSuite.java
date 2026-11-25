package com.quizapp.suites;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Runs exactly the tests that produce the 80 report tables (black-box and white-box),
 * regenerating target/test-tables/test-cases.md and .csv.
 */
@Suite
@SuiteDisplayName("All 80 report tables (R1-R20)")
@SelectPackages({"com.quizapp.blackbox", "com.quizapp.whitebox"})
public class ReportTablesSuite {
}
