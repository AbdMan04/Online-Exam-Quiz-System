package com.quizapp.support;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * JUnit 5/6 extension that turns test runs into report table rows.
 * <ul>
 *   <li>{@link ParameterResolver}: gives each test method a fresh {@link TestCase} to fill in.</li>
 *   <li>{@link AfterTestExecutionCallback}: adds Pass/Fail and stores the row.</li>
 * </ul>
 * All rows go to one {@link TestTableWriter} kept in the root store; JUnit closes it after the last
 * test, which writes the tables to {@code target/test-tables/}.
 */
public class TestCaseRecorder implements ParameterResolver, AfterTestExecutionCallback {

    private static final Namespace NAMESPACE = Namespace.create(TestCaseRecorder.class);
    private static final String CURRENT_CASE = "currentCase";
    private static final String WRITER = "writer";

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType() == TestCase.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        TestCase testCase = new TestCase();
        extensionContext.getStore(NAMESPACE).put(CURRENT_CASE, testCase);
        return testCase;
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        TestCase testCase = context.getStore(NAMESPACE).remove(CURRENT_CASE, TestCase.class);
        if (testCase == null || !testCase.isRecorded()) {
            return;
        }
        boolean passed = context.getExecutionException().isEmpty();
        writer(context).add(testCase.toRow(passed));
    }

    private static TestTableWriter writer(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE)
                .computeIfAbsent(WRITER, key -> new TestTableWriter(), TestTableWriter.class);
    }
}
