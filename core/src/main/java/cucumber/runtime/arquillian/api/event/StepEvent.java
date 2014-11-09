package cucumber.runtime.arquillian.api.event;

import cucumber.runtime.arquillian.shared.EventHelper;
import gherkin.formatter.model.Step;
import org.jboss.arquillian.test.spi.event.suite.TestEvent;

public abstract class StepEvent extends TestEvent {
    private final String featurePath;
    private final Step step;

    protected StepEvent(final String featurePath, final Step step) {
        super(EventHelper.currentEvent().getTestInstance(), EventHelper.currentEvent().getTestMethod());
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
