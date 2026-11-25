package com.quizapp.suites;

import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Runs every black-box requirement test (tables x-1 EP and x-2 BVA for R1-R20).
 * In IntelliJ: right-click this class and choose Run.
 */
@Suite
@SuiteDisplayName("Black-box tests - tables x-1 (EP) and x-2 (BVA)")
@SelectPackages("com.quizapp.blackbox")
@IncludeTags("blackbox")
public class BlackBoxSuite {
}
