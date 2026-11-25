package com.quizapp.junitfeatures;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

import java.time.Duration;

/**
 * Measures each test and publishes its run time as a report entry; marks tests slower than a threshold.
 * Registered programmatically with {@code @RegisterExtension} so the threshold can be configured per class.
 */
public class TimingExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

    private static final Namespace NAMESPACE = Namespace.create(TimingExtension.class);
    private static final String START = "start";

    private final Duration slowThreshold;

    public TimingExtension(Duration slowThreshold) {
        this.slowThreshold = slowThreshold;
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        context.getStore(NAMESPACE).put(START, System.nanoTime());
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        long start = context.getStore(NAMESPACE).remove(START, long.class);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
        String verdict = elapsed.compareTo(slowThreshold) > 0 ? " (SLOW)" : "";
        context.publishReportEntry("elapsedMs", elapsed.toMillis() + verdict);
    }
}
