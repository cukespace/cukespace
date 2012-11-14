package cucumber.runtime.arquillian.glue.ui;

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import com.thoughtworks.selenium.DefaultSelenium;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.test.api.ArquillianResource;

public class FunctionalBellySteps {
    @ArquillianResource
    private URL deploymentUrl;
    
    @Drone
    private DefaultSelenium browser;
    
    @When("^I eat (\\d+) cukes$")
    public void eatCukes(int cukes) {
        browser.type("id=bellyForm:mouth", Integer.toString(cukes));
        browser.captureScreenshot("target/screenshots/eatCukes.png");
        browser.click("id=bellyForm:eatCukes");
        browser.waitForPageToLoad("5000");
    }
    
    @Given("^I have a belly$")
    public void setUpBelly() throws MalformedURLException {
        browser.open(new URL(deploymentUrl, "faces/belly.xhtml").toString());
        browser.captureScreenshot("target/screenshots/setUpBelly.png");
        browser.waitForPageToLoad("5000");
    }
    
    @Then("^I should have (\\d+) cukes in my belly$")
    public void shouldHaveThisMany(int cukes) {
        browser.captureScreenshot("target/screenshots/shouldHaveThisMany.png");
        assertTrue(
            "Unexpected number of cukes!",
            browser.isElementPresent("xpath=//li[contains(text(), 'The belly ate " + cukes + " cukes!')]"));
    }
}
