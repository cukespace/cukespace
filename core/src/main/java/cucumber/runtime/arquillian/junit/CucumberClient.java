package cucumber.runtime.arquillian.junit;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.Test;

/**
 * JUnit fixture for running Cucumber features from the client.
 */
public abstract class CucumberClient extends Cucumber {
    
    /**
     * Initializes a new instance of the CucumberClient class.
     */
    public CucumberClient() {
        // intentionally empty
    }
    
    @Test
    @RunAsClient
    @Override
    public void runFeatures() throws Exception {
        super.runFeatures();
    }
}
