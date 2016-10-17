package cucumber.runtime.arquillian.testng;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import cucumber.runtime.arquillian.domain.Belly;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import javax.inject.Inject;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class ServerTestNGTest extends CukeSpace {
    @Deployment
    public static Archive<?> createDeployment() {
        return create(WebArchive.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addClass(Belly.class);
    }

    @Inject
    private Belly belly;

    @When("^I eat (\\d+) cukes$")
    public void eatCukes(int cukes) {
        belly.setCukes(cukes);
    }

    @Given("^I have a belly$")
    public void setUpBelly() {
        assertNotNull(belly);
    }

    @Then("^I should have (\\d+) cukes in my belly$")
    public void shouldHaveThisMany(int cukes) {
        assertEquals(cukes, belly.getCukes());
    }
}
