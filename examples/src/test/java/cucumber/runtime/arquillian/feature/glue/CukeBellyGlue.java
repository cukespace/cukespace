package cucumber.runtime.arquillian.feature.glue;

import com.thoughtworks.selenium.DefaultSelenium;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.test.api.ArquillianResource;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertTrue;

public class CukeBellyGlue {
    @ArquillianResource
    private URL deploymentUrl;

    @Drone
    protected DefaultSelenium browser;

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
