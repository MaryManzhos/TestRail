package utils;

import API.adapters.ProjectAdapter;
import API.adapters.ResultsAdapter;
import API.adapters.TestAdapter;
import API.adapters.TestRunAdapter;
import API.modelsAPI.TestResultAPI;
import API.modelsAPI.TestRunAPI;
import API.modelsAPI.TestRunsAPI;
import io.qameta.allure.Attachment;
import lombok.extern.log4j.Log4j2;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Log4j2
public class TestListener implements ITestListener {

    public static int testRailTestRunId;

    protected static void createTestRun(String nameOfProject) {
        ProjectAdapter projectAdapter = new ProjectAdapter();
        TestRunAdapter testRunAdapter = new TestRunAdapter();

        TestRunAPI testRun = TestRunAPI.builder()
                .name(Utils.generateNameOfTestRun())
                .description("test")
                .includeAll(true)
                .build();

        testRailTestRunId = testRunAdapter.addTestRun(projectAdapter.getProjectID(nameOfProject), testRun);
    }

    @Override
    public void onTestStart(ITestResult iTestResult) {
        log.debug("Starting test " + iTestResult.getName());
    }

    @Override
    public void onTestSuccess(ITestResult iTestResult) {
        log.debug("Finished test " + iTestResult.getName() + " duration: " + getExecutionTime(iTestResult));

        ResultsAdapter resultsAdapter = new ResultsAdapter();
        TestAdapter testAdapter = new TestAdapter();
        TestResultAPI testResultPassed = TestResultAPI.builder()
                .statusID(1)
                .build();

        for (int testCaseID : returnTestCaseID(iTestResult)) {
            resultsAdapter.addResult(testAdapter.getTestID(testCaseID, testRailTestRunId), testResultPassed);
        }
    }

    @Override
    public void onTestFailure(ITestResult iTestResult) {
        log.debug("Failed test " + iTestResult.getName() + " duration: " + getExecutionTime(iTestResult));

        takeScreenshot(iTestResult);

        String errorMessage = iTestResult.getThrowable().toString();
        ITestNGMethod testNGMethod = iTestResult.getMethod();
        String testRailComment = "Test - FAILED\n\nTest method name = " + testNGMethod.getMethodName() + "\n\nFailure Exception = " + errorMessage;

        ResultsAdapter resultsAdapter = new ResultsAdapter();
        TestAdapter testAdapter = new TestAdapter();
        TestResultAPI testResultPassed = TestResultAPI.builder()
                .statusID(5)
                .comment(testRailComment)
                .build();

        for (int testCaseID : returnTestCaseID(iTestResult)) {
            resultsAdapter.addResult(testAdapter.getTestID(testCaseID, testRailTestRunId), testResultPassed);
        }
    }

    @Override
    public void onTestSkipped(ITestResult iTestResult) {
        log.debug("Skipping test " + iTestResult.getName());
        takeScreenshot(iTestResult);
    }

    @Attachment(value = "Last screen state", type = "image/png")
    private byte[] takeScreenshot(ITestResult iTestResult) {
        ITestContext context = iTestResult.getTestContext();
        try {
            WebDriver driver = (WebDriver) context.getAttribute("driver");
            if (driver != null) {
                return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            } else {
                return new byte[]{};
            }
        } catch (NoSuchSessionException | IllegalStateException ex) {
            return new byte[]{};
        }
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult iTestResult) {

    }

    @Override
    public void onStart(ITestContext iTestContext) {
        if (testRailTestRunId == 0) {
            createTestRun("TestRailTest");
        } else {
            TestRunAdapter testRunAdapter = new TestRunAdapter();

            TestRunsAPI testRuns = testRunAdapter.getTestRun(testRailTestRunId);

            if (testRuns.isCompleted()) {
                createTestRun("TestRailTest");
            }
        }
    }

    @Override
    public void onFinish(ITestContext iTestContext) {
        TestRunAdapter testRunAdapter = new TestRunAdapter();

        TestRunsAPI testRuns = testRunAdapter.getTestRun(testRailTestRunId);

        if (testRuns.getUntestedCount() == 0) {
            testRunAdapter.closeTestRun(testRailTestRunId);
        }
    }

    private long getExecutionTime(ITestResult iTestResult) {
        return TimeUnit.MILLISECONDS.toSeconds(iTestResult.getEndMillis() - iTestResult.getStartMillis());
    }

    private int[] returnTestCaseID(ITestResult result) {
        ITestNGMethod testNGMethod = result.getMethod();
        Method method = testNGMethod.getConstructorOrMethod().getMethod();
        TestRail testRailAnnotation = method.getAnnotation(TestRail.class);
        int[] testCaseIDs;
        try {
            testCaseIDs = testRailAnnotation.testCaseID();
        } catch (Exception e) {
            return new int[]{};
        }
        return testCaseIDs;
    }

}
