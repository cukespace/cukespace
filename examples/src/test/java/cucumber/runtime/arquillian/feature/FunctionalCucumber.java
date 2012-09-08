package cucumber.runtime.arquillian.feature;

import org.jboss.arquillian.drone.api.annotation.Drone;

import com.thoughtworks.selenium.DefaultSelenium;

import cucumber.runtime.arquillian.junit.Cucumber;

/**
 * Example fixture for running client-side features.
 */
public abstract class FunctionalCucumber extends Cucumber {
    
    /**
     * The Selenium driver.
     * 
     * <p>HACK: This exists because Drone doesn't seem to create the Selenium
     * driver unless referenced explicitely by the test fixture. Without this
     * field, Drone will fail to inject Selenium into any glue code.</p>
     * 
     * <p>You are encouraged to implement a "client fixture" with the Drone
     * extension of your choice. You are not limited to DefaultSelenium.</p>
     */
    @Drone
    protected DefaultSelenium browser;
    
    /**
     * Initializes a new instance of the FunctionalCucumber class.
     */
    public FunctionalCucumber() {
        
        // intentionally empty
    }
}
