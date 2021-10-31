package cucumber.runtime.arquillian.api.event;

import gherkin.formatter.model.Step;
import org.jboss.arquillian.test.spi.event.suite.TestEvent;

public class AfterStep extends StepEvent {
    public AfterStep(final TestEvent event, final String featurePath, final Step step) {
        super(event, featurePath, step);
    }
}
