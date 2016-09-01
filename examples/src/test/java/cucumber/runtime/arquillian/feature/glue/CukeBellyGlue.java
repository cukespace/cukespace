package cucumber.runtime.arquillian.feature.glue;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import static org.jboss.arquillian.graphene.Graphene.waitAjax;
import static org.jboss.arquillian.graphene.Graphene.waitGui;
import static org.junit.Assert.assertEquals;
import static org.openqa.selenium.By.xpath;

public class CukeBellyGlue {
    @ArquillianResource
    private URL deploymentUrl;

    @Drone
    private WebDriver browser;

    @FindBy(id = "bellyForm:mouth")
    private WebElement bellyMouth;

    @FindBy(id = "bellyForm:eatCukes")
    private WebElement bellyEat;

    @When("^I eat (\\d+) cukes$")
    public void eatCukes(int cukes) throws IOException {
        waitAjax()/*we just loaded the page*/.until().element(bellyMouth).is().present();
        bellyMouth.sendKeys(Integer.toString(cukes));

        final File screenshot = new File("target/screenshots/eatCukes.png");
        screenshot.getParentFile().mkdirs();
        try (final OutputStream os = new FileOutputStream(screenshot)) {
            os.write(TakesScreenshot.class.cast(browser).getScreenshotAs(OutputType.BYTES));
        }

        waitGui()/*we are already on the page so faster wait cycle*/.until().element(bellyEat).is().present();
        bellyEat.click();
    }

    @Given("^I have a belly$")
    public void setUpBelly() throws MalformedURLException {
        browser.get(deploymentUrl.toExternalForm() + "faces/belly.xhtml");
    }

    @Then("^I should have (\\d+) cukes in my belly$")
    public void shouldHaveThisMany(final int cukes) {
        final WebElement element = browser.findElement(xpath("//li[contains(text(), 'The belly ate ')]"));
        waitAjax().until().element(element).is().present();
        assertEquals("The belly ate " + cukes + " cukes!", browser.findElement(xpath("//li[contains(text(), 'The belly ate')]")).getText());
    }
}
