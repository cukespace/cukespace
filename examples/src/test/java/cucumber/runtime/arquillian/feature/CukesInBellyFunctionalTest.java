package cucumber.runtime.arquillian.feature;

import com.thoughtworks.selenium.DefaultSelenium;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import cucumber.runtime.arquillian.ArquillianCucumber;
import cucumber.runtime.arquillian.controller.BellyController;
import cucumber.runtime.arquillian.domain.Belly;
import cucumber.runtime.arquillian.producer.FacesContextProducer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertTrue;

@RunWith(ArquillianCucumber.class)
public class CukesInBellyFunctionalTest {
    @Deployment(testable = false)
    public static Archive<?> createDeployment() {
        return create(WebArchive.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsWebInfResource(new StringAsset("<faces-config version=\"2.0\"/>"), "faces-config.xml")
            .addAsWebResource(new File("src/main/webapp/belly.xhtml"), "belly.xhtml")
            .addClass(Belly.class)
            .addClass(BellyController.class)
            .addClass(FacesContextProducer.class);
    }

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
