package com.quizapp.suites;

import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Runs every white-box requirement test (tables x-3 statement and x-4 branch coverage for R1-R20).
 * Run it "with Coverage" in IntelliJ to see the coverage reached by white-box tests alone.
 */
@Suite
@SuiteDisplayName("White-box tests - tables x-3 (statement) and x-4 (branch)")
@SelectPackages("com.quizapp.whitebox")
@IncludeTags("whitebox")
public class WhiteBoxSuite {
}
