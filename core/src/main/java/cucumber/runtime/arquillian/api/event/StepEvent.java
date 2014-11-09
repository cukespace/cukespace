package cucumber.runtime.arquillian.api.event;

import gherkin.formatter.model.Step;

public abstract class StepEvent {
    private final String featurePath;
    private final Step step;

    protected StepEvent(final String featurePath, final Step step) {
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
