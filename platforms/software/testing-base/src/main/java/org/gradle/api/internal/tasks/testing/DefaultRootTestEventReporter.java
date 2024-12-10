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

package org.gradle.api.internal.tasks.testing;

import org.gradle.api.NonNullApi;
import org.gradle.api.internal.tasks.testing.results.HtmlTestReportGenerator;
import org.gradle.api.internal.tasks.testing.results.TestExecutionResultsListener;
import org.gradle.api.internal.tasks.testing.results.TestListenerInternal;
import org.gradle.api.tasks.VerificationException;
import org.gradle.internal.id.IdGenerator;
import org.gradle.internal.logging.ConsoleRenderer;
import org.gradle.util.internal.TextUtil;

import java.nio.file.Path;
import java.time.Instant;

@NonNullApi
class DefaultRootTestEventReporter extends DefaultGroupTestEventReporter {

    private final Path testReportDirectory;
    private final DefaultTestEventReporterFactory.ClosableTestReportDataCollector testReportDataCollector;
    private final HtmlTestReportGenerator htmlTestReportGenerator;
    private final TestExecutionResultsListener executionResultsListener;

    // Mutable state
    private String failureMessage;

    DefaultRootTestEventReporter(
        String rootName,
        TestListenerInternal listener,
        IdGenerator<?> idGenerator,
        Path testReportDirectory,
        DefaultTestEventReporterFactory.ClosableTestReportDataCollector testReportDataCollector,
        HtmlTestReportGenerator htmlTestReportGenerator,
        TestExecutionResultsListener executionResultsListener
    ) {
        super(
            listener,
            idGenerator,
            new DefaultTestSuiteDescriptor(idGenerator.generateId(), rootName),
            new TestResultState(null)
        );

        this.testReportDirectory = testReportDirectory;
        this.testReportDataCollector = testReportDataCollector;
        this.htmlTestReportGenerator = htmlTestReportGenerator;
        this.executionResultsListener = executionResultsListener;
    }

    @Override
    public void close() {
        super.close();

        // Ensure binary results are written to disk.
        testReportDataCollector.close();
        Path binaryResultsDir = testReportDataCollector.getBinaryResultsDirectory();

        boolean rootTestFailed = failureMessage != null;

        // Notify aggregate listener of final results
        executionResultsListener.executionResultsAvailable(testDescriptor, binaryResultsDir, rootTestFailed);

        // Generate HTML report
        Path reportIndexFile = htmlTestReportGenerator.generateHtmlReport(testReportDirectory, binaryResultsDir);

        // Throw an exception with rendered test results, if necessary
        if (rootTestFailed) {
            String testResultsUrl = new ConsoleRenderer().asClickableFileUrl(reportIndexFile.toFile());
            throw new VerificationException(failureMessage + " See the test results for more details: " + testResultsUrl);
        }
    }

    @Override
    public void failed(Instant endTime, String message, String additionalContent) {
        if (TextUtil.isBlank(message)) {
            this.failureMessage = "Test(s) failed.";
        } else {
            this.failureMessage = message;
        }
        super.failed(endTime, message, additionalContent);
    }
}
