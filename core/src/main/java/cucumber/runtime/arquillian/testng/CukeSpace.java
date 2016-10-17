package cucumber.runtime.arquillian.testng;

import cucumber.api.testng.FeatureResultListener;
import cucumber.runtime.CucumberException;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.arquillian.runner.BaseCukeSpace;
import cucumber.runtime.model.CucumberFeature;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Background;
import gherkin.formatter.model.Examples;
import gherkin.formatter.model.Feature;
import gherkin.formatter.model.Match;
import gherkin.formatter.model.Result;
import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.ScenarioOutline;
import gherkin.formatter.model.Step;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public class CukeSpace extends Arquillian {
    @Test
    public void cukeSpaceDoRun() throws Exception {
        new TestNGCukeSpace(getClass()).performInternalCucumberOperations(this, null);
    }

    public static class TestNGCukeSpace extends BaseCukeSpace<FormaterReporterFacade, Void> {
        private final Class<?> type;

        private TestNGCukeSpace(final Class<? extends CukeSpace> type) {
            this.type = type;
        }

        @Override
        protected Class<?> getTestedClass() {
            return type;
        }

        @Override
        protected Exception newMultipleFailureException(final List<Throwable> errors) {
            if (errors.size() == 1 && Exception.class.isInstance(errors.get(0))) {
                return Exception.class.cast(errors.get(0));
            }

            final StringBuilder error = new StringBuilder();
            for (final Throwable t : errors) {
                final StringWriter writer = new StringWriter();
                final PrintWriter s = new PrintWriter(writer);
                t.printStackTrace(s);
                s.close();
                error.append(t.getMessage()).append("\n").append(writer.toString()).append("\n\n");
            }
            return new IllegalStateException(error.toString());
        }

        @Override
        protected void runFeature(final CucumberFeature feature, final BaseCukeSpace.CucumberRuntime cucumberRuntime,
                                  final FormaterReporterFacade reporter, final Void runNotifier) throws Throwable {
            reporter.reporter.startFeature();
            feature.run(reporter.formatter, reporter.reporter, cucumberRuntime);
            reporter.formatter.done();
            reporter.formatter.close();
            cucumberRuntime.printSummary();
            if (!reporter.reporter.isPassed()) {
                throw new CucumberException(reporter.reporter.getFirstError());
            }
        }

        @Override
        protected FormaterReporterFacade getReporter(final Reporter delegate, final Formatter formatter, final RuntimeOptions runtimeOptions) {
            return new FormaterReporterFacade(new FeatureResultListener(delegate, runtimeOptions.isStrict()), formatter);
        }
    }

    public static class FormaterReporterFacade implements Reporter, Formatter {
        private final FeatureResultListener reporter;
        private final Formatter formatter;

        private FormaterReporterFacade(final FeatureResultListener reporter, final Formatter formatter) {
            this.reporter = reporter;
            this.formatter = formatter;
        }

        @Override
        public void syntaxError(final String state, final String event,
                                final List<String> legalEvents, final String uri, final Integer line) {
            formatter.syntaxError(state, event, legalEvents, uri, line);
        }

        @Override
        public void uri(final String uri) {
            formatter.uri(uri);
        }

        @Override
        public void feature(final Feature feature) {
            formatter.feature(feature);
        }

        @Override
        public void scenarioOutline(final ScenarioOutline scenarioOutline) {
            formatter.scenarioOutline(scenarioOutline);
        }

        @Override
        public void examples(final Examples examples) {
            formatter.examples(examples);
        }

        @Override
        public void startOfScenarioLifeCycle(final Scenario scenario) {
            formatter.startOfScenarioLifeCycle(scenario);
        }

        @Override
        public void background(final Background background) {
            formatter.background(background);
        }

        @Override
        public void scenario(final Scenario scenario) {
            formatter.scenario(scenario);
        }

        @Override
        public void step(final Step step) {
            formatter.step(step);
        }

        @Override
        public void endOfScenarioLifeCycle(final Scenario scenario) {
            formatter.endOfScenarioLifeCycle(scenario);
        }

        @Override
        public void done() {
            formatter.done();
        }

        @Override
        public void close() {
            formatter.close();
        }

        @Override
        public void eof() {
            formatter.eof();
        }

        @Override
        public void before(final Match match, final Result result) {
            reporter.before(match, result);
        }

        @Override
        public void result(final Result result) {
            reporter.result(result);
        }

        @Override
        public void after(final Match match, final Result result) {
            reporter.after(match, result);
        }

        @Override
        public void match(final Match match) {
            reporter.match(match);
        }

        @Override
        public void embedding(final String mimeType, final byte[] data) {
            reporter.embedding(mimeType, data);
        }

        @Override
        public void write(final String text) {
            reporter.write(text);
        }
    }
}
