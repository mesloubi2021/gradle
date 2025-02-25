/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.tasks.testing.results;

import org.gradle.api.NonNullApi;
import org.gradle.api.internal.tasks.testing.TestDescriptorInternal;
import org.gradle.api.internal.tasks.testing.junit.result.AggregateTestResultsProvider;
import org.gradle.api.internal.tasks.testing.junit.result.BinaryResultBackedTestResultsProvider;
import org.gradle.api.internal.tasks.testing.junit.result.TestResultsProvider;
import org.gradle.api.internal.tasks.testing.report.HtmlTestReport;
import org.gradle.internal.concurrent.CompositeStoppable;
import org.gradle.internal.logging.ConsoleRenderer;
import org.gradle.internal.operations.BuildOperationExecutor;
import org.gradle.internal.operations.BuildOperationRunner;
import org.gradle.problems.buildtree.ProblemReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Aggregates test results from multiple test executions and generates a report at the end of the build.
 */
@NonNullApi
public class AggregateTestEventReporter implements ProblemReporter, TestExecutionResultsListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AggregateTestEventReporter.class);

    private final BuildOperationRunner buildOperationRunner;
    private final BuildOperationExecutor buildOperationExecutor;

    // Mutable state
    private final AtomicInteger numFailedResults = new AtomicInteger(0);
    private final Map<TestDescriptorInternal, Path> results = new ConcurrentHashMap<>();

    @Inject
    public AggregateTestEventReporter(BuildOperationRunner buildOperationRunner, BuildOperationExecutor buildOperationExecutor) {
        this.buildOperationRunner = buildOperationRunner;
        this.buildOperationExecutor = buildOperationExecutor;
    }

    @Override
    public String getId() {
        return "aggregate-test-results";
    }

    @Override
    public void executionResultsAvailable(TestDescriptorInternal rootDescriptor, Path binaryResultsDir, boolean hasFailures) {
        results.put(rootDescriptor, binaryResultsDir);
        if (hasFailures) {
            numFailedResults.incrementAndGet();
        }
    }

    @Override
    public void report(File reportDir, ProblemConsumer validationFailures) {
        Path reportLocation = reportDir.toPath().resolve("reports").resolve("aggregate-test-results");

        if (!results.isEmpty()) {
            Path reportIndexFile = generateTestReport(reportLocation);

            // Print report to console only if there are failures and if we have multiple results to aggregate.
            if (numFailedResults.get() > 1) {
                emitReport(reportIndexFile);
            }
        }

    }

    /**
     * Generates an aggregate test report at the given directory.
     *
     * @return The path to the index file that should be reported to the user.
     */
    private Path generateTestReport(Path reportDirectory) {
        return withTestResults(providers -> {
            HtmlTestReport htmlReport = new HtmlTestReport(buildOperationRunner, buildOperationExecutor);
            htmlReport.generateReport(new AggregateTestResultsProvider(providers), reportDirectory.toFile());
            return reportDirectory.resolve("index.html");
        });
    }

    private <T> T withTestResults(Function<List<TestResultsProvider>, T> action) {
        List<TestResultsProvider> providers = new ArrayList<>();
        try {
            for (Path result : results.values()) {
                providers.add(new BinaryResultBackedTestResultsProvider(result.toFile()));
            }
            return action.apply(providers);
        } finally {
            CompositeStoppable.stoppable(providers).stop();
        }
    }

    /**
     * Emits the report to the user.
     *
     * @param reportIndexFile The path to report to the user as a link.
     */
    private static void emitReport(Path reportIndexFile) {
        String url = new ConsoleRenderer().asClickableFileUrl(reportIndexFile.toFile());

        // TODO: Integrate with Problems API report or some new "build dashboard 2.0"
        // We should avoid printing more than one link at the end of the build.
        LOGGER.warn("Aggregate test results: {}", url);
    }
}
