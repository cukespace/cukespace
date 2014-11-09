package cucumber.runtime.arquillian.api.event;

import gherkin.formatter.model.Step;

public class AfterStep extends StepEvent {
    public AfterStep(final String featurePath, final Step step) {
        super(featurePath, step);
    }
}
