package cucumber.runtime.arquillian.api.event;

import gherkin.formatter.model.Step;

public class BeforeStep extends StepEvent {
    public BeforeStep(final String featurePath, final Step step) {
        super(featurePath, step);
    }
}
