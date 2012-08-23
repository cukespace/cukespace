package cucumber.runtime.arquillian.junit;

import org.jboss.arquillian.drone.api.annotation.Drone;

import com.thoughtworks.selenium.DefaultSelenium;

/**
 * JUnit fixture for running Cucumber features from the client.
 */
public abstract class CucumberClient extends Cucumber {
    
    /**
     * The Selenium driver.
     * 
     * <p>HACK: This exists because Drone doesn't seem to create the Selenium
     * driver unless referenced directly by the test fixture. Without this
     * field, Drone won't inject Selenium into any glue code.</p>
     */
    @Drone
    protected DefaultSelenium selenium;
    
    /**
     * Initializes a new instance of the CucumberClient class.
     */
    public CucumberClient() {
        // intentionally empty
    }
}
