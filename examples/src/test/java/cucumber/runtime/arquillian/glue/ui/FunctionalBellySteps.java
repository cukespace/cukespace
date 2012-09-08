package cucumber.runtime.arquillian.glue.ui;

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.test.api.ArquillianResource;

import com.thoughtworks.selenium.DefaultSelenium;

import cucumber.annotation.en.Given;
import cucumber.annotation.en.Then;
import cucumber.annotation.en.When;

/**
 * Test steps for the cuke-eating belly.
 */
public class FunctionalBellySteps {
    
    /**
     * The application URL.
     */
    @ArquillianResource
    private URL deploymentUrl;
    
    /**
     * The Selenium driver.
     */
    @Drone
    private DefaultSelenium browser;
    
    /**
     * Initializes a new instance of the BellySteps class.
     */
    public FunctionalBellySteps() {
        // intentionally empty
    }
    
    /**
     * Eats some cukes.
     * 
     * @param cukes The number of cukes.
     */
    @When("^I eat (\\d+) cukes$")
    public void eatCukes(int cukes) {
        
        this.browser.type("id=bellyForm:mouth", Integer.toString(cukes));
        this.browser.captureScreenshot("target/screenshots/eatCukes.png");
        this.browser.click("id=bellyForm:eatCukes");
        this.browser.waitForPageToLoad("5000");
    }
    
    /**
     * Sets up the belly for eating cukes.
     * 
     * @throws MalformedURLException Thrown if the URL to the belly is
     * malformed.
     */
    @Given("^I have a belly$")
    public void setUpBelly() throws MalformedURLException {
        
        this.browser.open(new URL(this.deploymentUrl, "faces/belly.xhtml").toString());
        this.browser.captureScreenshot("target/screenshots/setUpBelly.png");
        this.browser.waitForPageToLoad("5000");
    }
    
    /**
     * Verifies the number of cukes.
     * 
     * @param cukes The expected number of cukes.
     */
    @Then("^I should have (\\d+) cukes in my belly$")
    public void shouldHaveThisMany(int cukes) {
        
        this.browser.captureScreenshot("target/screenshots/shouldHaveThisMany.png");
        
        assertTrue(
            "Unexpected number of cukes!",
            this.browser.isElementPresent("xpath=//li[contains(text(), 'The belly ate " + cukes + " cukes!')]")
        );
    }
}
