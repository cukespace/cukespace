package cucumber.runtime.arquillian.api.event;

import gherkin.formatter.model.Step;
import org.jboss.arquillian.test.spi.event.suite.TestEvent;

public abstract class StepEvent extends TestEvent {
    private final String featurePath;
    private final Step step;

    protected StepEvent(final TestEvent event, final String featurePath, final Step step) {
        super(event.getTestInstance(), event.getTestMethod());
        this.featurePath = featurePath;
        this.step = step;
    }

    public String getFeaturePath() {
        return featurePath;
    }

    public Step getStep() {
        return step;
    }
}
